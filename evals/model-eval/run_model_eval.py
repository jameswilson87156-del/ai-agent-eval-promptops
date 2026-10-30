"""Run a bounded, repeatable model evaluation through an OpenAI-compatible gateway.

This CLI is intentionally separate from the deterministic Mock Agent dataset and
from the production Spring Boot run API. It requires an explicit draft opt-in,
uses no retries or tools, and writes a credential-free JSON evidence snapshot.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
from http.client import HTTPException
import json
import math
import os
from pathlib import Path
import re
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
import uuid


DEFAULT_MODELS = (
    "glm-5.3-flash",
    "deepseek-v4.1-flash",
    "kimi-k3.1-pro",
    "minimax-m3.1-flash",
    "step-5-preview",
)
MODEL_GROUP_BY_ID = {
    "deepseek-v4-pro": "guomodel",
    "glm-5.3-flash": "guomodel",
    "deepseek-v4.1-flash": "guomodel",
    "glm-5.1": "guomodel",
    "glm-5.2": "guomodel",
    "glm-5.3": "guomodel",
    "hy3": "guomodel",
    "hy4-preview": "guomodel",
    "kimi-k2.8-preview": "guomodel",
    "kimi-k3-1": "guomodel",
    "kimi-k3.1-pro": "guomodel",
    "mimo-v2.6-pro": "guomodel",
    "minimax-m3": "guomodel",
    "minimax-m3.1-flash": "guomodel",
    "step-5-preview": "guomodel",
    "gemini-3.8-flash-low": "gemini",
}
DEFAULT_BASE_URL = "https://ai.xem8k5.top/v1"
MAX_MODELS = 20
MAX_REQUESTS = 5000
DEFAULT_REQUEST_CAP = 200
MAX_REPETITIONS = 20
MAX_OUTPUT_TOKENS = 8192
DEFAULT_OUTPUT_TOKENS = 1024
MAX_TIMEOUT_SECONDS = 300
DEFAULT_TIMEOUT_SECONDS = 45
MAX_BUDGET_YUAN = 500.0
DEFAULT_BUDGET_YUAN = 100.0
MAX_RESPONSE_BYTES = 1_000_000
MAX_ERROR_BODY_BYTES = 4096
MAX_ERROR_DETAIL_CHARS = 300


class ModelEvalError(Exception):
    def __init__(self, code: str, message: str, *, safe_detail: str | None = None):
        super().__init__(message)
        self.code = code
        self.safe_detail = safe_detail


def normalize_endpoint(base_url: str) -> str:
    parsed = urlsplit((base_url or "").strip())
    host = (parsed.hostname or "").lower()
    is_local_http = parsed.scheme == "http" and host in {"localhost", "127.0.0.1", "::1"}
    if not host or (parsed.scheme != "https" and not is_local_http):
        raise ValueError("Provider endpoint must use HTTPS; HTTP is allowed only for loopback tests.")
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError("Provider endpoint must not contain credentials, query parameters, or a fragment.")
    base_path = parsed.path.rstrip("/")
    if base_path.endswith("/chat/completions"):
        path = base_path
    else:
        path = f"{base_path}/chat/completions"
    return f"{parsed.scheme}://{parsed.netloc}{path}"


def normalize_catalog_endpoint(base_url: str) -> str:
    parsed = urlsplit((base_url or "").strip())
    host = (parsed.hostname or "").lower()
    is_local_http = parsed.scheme == "http" and host in {"localhost", "127.0.0.1", "::1"}
    if not host or (parsed.scheme != "https" and not is_local_http):
        raise ValueError("Provider endpoint must use HTTPS; HTTP is allowed only for loopback tests.")
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError("Provider endpoint must not contain credentials, query parameters, or a fragment.")
    base_path = parsed.path.rstrip("/")
    if base_path.endswith("/chat/completions"):
        base_path = base_path[:-len("/chat/completions")]
    if not base_path.endswith("/models"):
        base_path = f"{base_path}/models"
    return f"{parsed.scheme}://{parsed.netloc}{base_path}"


def model_group_for(model: str) -> str:
    """Model groups inferred from the user's XEM catalog screenshots."""
    return MODEL_GROUP_BY_ID.get(model, "default")


def configured_api_key(environment: dict | None = None, *, model: str | None = None) -> str:
    """Read a dedicated gateway/group credential; never fall back to a vendor key."""
    environment = os.environ if environment is None else environment
    group = model_group_for(model) if model else None
    group_key = f"PROMPTOPS_PROVIDER_API_KEY_{group.upper()}" if group else ""
    for variable in (group_key, "PROMPTOPS_PROVIDER_API_KEY"):
        if not variable:
            continue
        value = environment.get(variable, "")
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def configured_catalog_credentials(environment: dict | None = None) -> list[tuple[str, str]]:
    """Use known group keys individually, or query once with a generic gateway key."""
    environment = os.environ if environment is None else environment
    groups = list(dict.fromkeys(MODEL_GROUP_BY_ID.values()))
    credentials = []
    for group in groups:
        value = environment.get(f"PROMPTOPS_PROVIDER_API_KEY_{group.upper()}", "")
        if isinstance(value, str) and value.strip():
            credentials.append((group, value.strip()))
    if credentials:
        return credentials
    value = environment.get("PROMPTOPS_PROVIDER_API_KEY", "")
    if isinstance(value, str) and value.strip():
        return [("gateway-visible", value.strip())]
    return []


def call_model_catalog(base_url: str, api_key: str, *, timeout_seconds: int = 20) -> list[str]:
    """Read model IDs visible to this credential through the OpenAI-compatible catalog endpoint."""
    endpoint = normalize_catalog_endpoint(base_url)
    if not api_key or not api_key.strip():
        raise ModelEvalError("missing_credentials", "No gateway credential is configured.")
    request = Request(endpoint, headers={"Authorization": f"Bearer {api_key}", "Accept": "application/json"}, method="GET")
    try:
        with urlopen(request, timeout=timeout_seconds) as response:
            raw = response.read(MAX_RESPONSE_BYTES + 1)
            if len(raw) > MAX_RESPONSE_BYTES:
                raise ModelEvalError("response_too_large", "Gateway model catalog exceeded the 1 MB safety limit.")
    except HTTPError as error:
        status = error.code
        code = "gateway_auth_error" if status in (401, 403) else "gateway_rate_limited" if status == 429 else f"gateway_http_{status}"
        raise ModelEvalError(code, f"Gateway returned HTTP {status}; response body was not retained.") from None
    except URLError:
        raise ModelEvalError("gateway_network_error", "Gateway model catalog request failed; connection details were not retained.") from None
    except TimeoutError:
        raise ModelEvalError("gateway_timeout", "Gateway model catalog request timed out.") from None
    try:
        envelope = json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise ModelEvalError("gateway_invalid_json", "Gateway model catalog returned a non-JSON response body.") from None
    entries = envelope.get("data") if isinstance(envelope, dict) else None
    if not isinstance(entries, list):
        raise ModelEvalError("gateway_invalid_response", "Gateway model catalog did not contain a data list.")
    model_ids = sorted({entry["id"].strip() for entry in entries
                        if isinstance(entry, dict) and isinstance(entry.get("id"), str) and entry["id"].strip()})
    if not model_ids:
        raise ModelEvalError("gateway_empty_catalog", "Gateway model catalog contained no model IDs visible to this key.")
    return model_ids


def validate_run_limits(*, models: list[str], case_count: int, max_requests: int,
                        max_output_tokens: int, budget_yuan: float, repetitions: int = 1,
                        timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS) -> None:
    if not models or len(models) > MAX_MODELS or len(set(models)) != len(models):
        raise ValueError(f"Select 1 to {MAX_MODELS} distinct models.")
    if case_count < 1:
        raise ValueError("Dataset must contain at least one case.")
    if repetitions < 1 or repetitions > MAX_REPETITIONS:
        raise ValueError(f"Repetitions must be between 1 and {MAX_REPETITIONS}.")
    requested_calls = len(models) * case_count * repetitions
    if max_requests < requested_calls or max_requests > MAX_REQUESTS:
        raise ValueError(f"The request cap must cover this run and cannot exceed {MAX_REQUESTS} calls.")
    if max_output_tokens < 1 or max_output_tokens > MAX_OUTPUT_TOKENS:
        raise ValueError(f"Output token cap must be between 1 and {MAX_OUTPUT_TOKENS}.")
    if isinstance(timeout_seconds, bool) or not isinstance(timeout_seconds, int) \
            or timeout_seconds < 1 or timeout_seconds > MAX_TIMEOUT_SECONDS:
        raise ValueError(f"Gateway timeout must be between 1 and {MAX_TIMEOUT_SECONDS} seconds.")
    if not math.isfinite(budget_yuan) or budget_yuan <= 0 or budget_yuan > MAX_BUDGET_YUAN:
        raise ValueError(f"Authorized budget must be greater than 0 and no more than ¥{MAX_BUDGET_YUAN:g}.")


def _safe_usage(usage: object) -> dict | None:
    if not isinstance(usage, dict):
        return None
    result = {}
    token_names = {
        "prompt_tokens", "completion_tokens", "total_tokens", "input_tokens", "output_tokens",
        "cached_tokens", "reasoning_tokens", "cache_read_input_tokens", "cache_creation_input_tokens",
    }
    for key in token_names:
        value = usage.get(key)
        if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
            result[key] = value
    for key in ("cost", "total_cost"):
        value = usage.get(key)
        if isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value) and value >= 0:
            result[key] = value
    currency = usage.get("currency")
    if isinstance(currency, str) and re.fullmatch(r"[A-Za-z]{3}", currency):
        result["currency"] = currency.upper()
    return result or None


def _text_length(value: object) -> int | None:
    if isinstance(value, str):
        return len(value)
    if isinstance(value, list):
        text_parts = [part["text"] for part in value
                      if isinstance(part, dict) and isinstance(part.get("text"), str)]
        return sum(len(part) for part in text_parts)
    return None


def call_chat_completion(base_url: str, api_key: str, model: str, messages: list[dict], *,
                         max_output_tokens: int = DEFAULT_OUTPUT_TOKENS,
                         timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS) -> dict:
    endpoint = normalize_endpoint(base_url)
    if not api_key or not api_key.strip():
        raise ModelEvalError("missing_credentials", "No gateway credential is configured.")
    if not model or not model.strip():
        raise ModelEvalError("missing_model", "Requested model name is empty.")
    body = {
        "model": model,
        "messages": messages,
        "temperature": 0,
        "max_tokens": max_output_tokens,
        "stream": False,
    }
    request = Request(
        endpoint,
        data=json.dumps(body, ensure_ascii=False).encode("utf-8"),
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        method="POST",
    )
    started = time.perf_counter()
    try:
        with urlopen(request, timeout=timeout_seconds) as response:
            raw = response.read(MAX_RESPONSE_BYTES + 1)
            if len(raw) > MAX_RESPONSE_BYTES:
                raise ModelEvalError("response_too_large", "Gateway response exceeded the 1 MB safety limit.")
            response_headers = response.headers
    except HTTPError as error:
        status = error.code
        code = "gateway_auth_error" if status in (401, 403) else "gateway_rate_limited" if status == 429 else f"gateway_http_{status}"
        safe_detail = None
        try:
            if status not in (401, 403):
                error_body = error.read(MAX_ERROR_BODY_BYTES + 1)
                if len(error_body) <= MAX_ERROR_BODY_BYTES:
                    safe_detail = _safe_gateway_error_detail(error_body, api_key)
        except (HTTPException, OSError, RecursionError, ValueError):
            safe_detail = None
        finally:
            error.close()
        raise ModelEvalError(
            code,
            f"Gateway returned HTTP {status}; raw response body was not retained.",
            safe_detail=safe_detail,
        ) from None
    except URLError:
        raise ModelEvalError("gateway_network_error", "Gateway request failed; connection details were not retained.") from None
    except TimeoutError:
        raise ModelEvalError("gateway_timeout", "Gateway request timed out.") from None
    duration_ms = round((time.perf_counter() - started) * 1000, 3)
    try:
        envelope = json.loads(raw)
    except (UnicodeDecodeError, json.JSONDecodeError):
        raise ModelEvalError("gateway_invalid_json", "Gateway returned a non-JSON response body.") from None
    choices = envelope.get("choices") if isinstance(envelope, dict) else None
    if not isinstance(choices, list) or not choices or not isinstance(choices[0], dict):
        raise ModelEvalError("gateway_invalid_response", "Gateway response did not contain a completion choice.")
    message = choices[0].get("message")
    content = message.get("content") if isinstance(message, dict) else None
    original_content_type = type(content).__name__ if content is not None else "null"
    if isinstance(content, list):
        content = "".join(part.get("text", "") for part in content if isinstance(part, dict) and isinstance(part.get("text"), str))
    if not isinstance(content, str):
        raise ModelEvalError("gateway_empty_response", "Gateway completion did not contain text content.")
    headers = response_headers
    reasoning_content = message.get("reasoning_content") if isinstance(message, dict) else None
    tool_calls = message.get("tool_calls") if isinstance(message, dict) else None
    refusal = message.get("refusal") if isinstance(message, dict) else None
    return {
        "content": content,
        "usage": _safe_usage(envelope.get("usage")),
        "returned_model": envelope.get("model") if isinstance(envelope.get("model"), str) else None,
        "request_id": headers.get("x-request-id") or headers.get("request-id") or envelope.get("id"),
        "finish_reason": choices[0].get("finish_reason"),
        "latency_ms": duration_ms,
        "response_diagnostics": {
            "message_content_type": original_content_type,
            "message_content_chars": len(content),
            "reasoning_content_present": reasoning_content not in (None, "", []),
            "reasoning_content_chars": _text_length(reasoning_content),
            "tool_calls_present": isinstance(tool_calls, list) and bool(tool_calls),
            "refusal_present": refusal not in (None, "", []),
        },
    }


def _resolve_path(value: object, path: str):
    if not path or path == "$":
        return value
    if not path.startswith("$."):
        return None
    current = value
    for component in path[2:].split("."):
        if isinstance(current, dict):
            current = current.get(component)
        elif isinstance(current, list) and component.isdecimal():
            index = int(component)
            current = current[index] if index < len(current) else None
        else:
            return None
    return current


def _matches_type(value: object, expected_type: str | list) -> bool:
    if isinstance(expected_type, list):
        return any(_matches_type(value, candidate) for candidate in expected_type)
    if expected_type == "object":
        return isinstance(value, dict)
    if expected_type == "array":
        return isinstance(value, list)
    if expected_type == "string":
        return isinstance(value, str)
    if expected_type == "boolean":
        return isinstance(value, bool)
    if expected_type == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    if expected_type == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    return value is None and expected_type == "null"


def _reject_nonstandard_json_constant(token: str):
    raise ValueError(f"Non-standard JSON numeric constant: {token}")


def _json_object_reject_duplicate_keys(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON object key: {key}")
        result[key] = value
    return result


def evaluate_response(case: dict, raw_output: str) -> list[dict]:
    try:
        parsed = json.loads(raw_output, parse_constant=_reject_nonstandard_json_constant,
                            object_pairs_hook=_json_object_reject_duplicate_keys)
        parse_error = None
    except (json.JSONDecodeError, TypeError, ValueError):
        parsed = None
        parse_error = "Output is not valid JSON."
    results = []
    for check in case.get("deterministic_checks", []):
        kind = check.get("kind")
        expected = check.get("expected")
        path = check.get("path", "")
        actual = _resolve_path(parsed, path) if parsed is not None else None
        passed = False
        if kind == "json_schema" and parsed is not None and isinstance(expected, dict):
            required = expected.get("required", {})
            passed = isinstance(parsed, dict) and isinstance(required, dict) and all(
                key in parsed and _matches_type(parsed[key], value) for key, value in required.items()
            )
            property_rules = expected.get("properties", {})
            if passed and isinstance(property_rules, dict):
                for key, constraints in property_rules.items():
                    if not isinstance(constraints, dict) or key not in parsed:
                        continue
                    if "type" in constraints and not _matches_type(parsed[key], constraints["type"]):
                        passed = False
                    if "enum" in constraints and parsed[key] not in constraints["enum"]:
                        passed = False
            if expected.get("additional_properties") is False and isinstance(parsed, dict):
                passed = passed and set(parsed) == set(required)
        elif kind == "business_rule":
            passed = parsed is not None and actual == expected
        elif kind == "contains":
            target = raw_output if not path or path == "$raw" else actual
            actual = isinstance(target, str) and isinstance(expected, str) and expected in target
            passed = actual
        elif kind == "not_contains":
            target = raw_output if not path or path == "$raw" else actual
            actual = isinstance(target, str) and isinstance(expected, str) and expected in target
            passed = isinstance(target, str) and isinstance(expected, str) and not actual
        results.append({
            "check_id": check.get("check_id"),
            "kind": kind,
            "passed": bool(passed),
            "expected": expected,
            "actual": actual if parsed is not None or kind in {"contains", "not_contains"} else parse_error,
            "description": check.get("description", ""),
        })
    return results


def _redact(value: str, api_key: str) -> str:
    if api_key:
        value = value.replace(api_key, "[REDACTED]")
    value = re.sub(r"(?i)Bearer\s+\S+", "Bearer [REDACTED]", value)
    value = re.sub(r"(?i)(api[_-]?key|authorization|token|secret|password)\s*[:=]\s*[^\s,;]+", r"\1=[REDACTED]", value)
    value = re.sub(r"\bsk-[A-Za-z0-9_-]{16,}\b", "[REDACTED]", value)
    return value


def _sanitize_error_detail(value: str, api_key: str) -> str | None:
    if not isinstance(value, str) or not value.strip():
        return None
    sanitized = "".join(" " if ord(char) < 32 or ord(char) == 127 else char for char in value)
    sanitized = re.sub(r"\s+", " ", _redact(sanitized, api_key)).strip()
    if not sanitized:
        return None
    if len(sanitized) > MAX_ERROR_DETAIL_CHARS:
        sanitized = sanitized[:MAX_ERROR_DETAIL_CHARS - 1].rstrip() + "…"
    return sanitized


def _safe_gateway_error_detail(error_body: bytes, api_key: str) -> str | None:
    """Return a bounded summary of allowlisted JSON error fields only."""
    if not error_body or len(error_body) > MAX_ERROR_BODY_BYTES:
        return None
    try:
        envelope = json.loads(error_body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return None
    if not isinstance(envelope, dict):
        return None

    error = envelope.get("error")
    if isinstance(error, dict):
        source = error
    elif isinstance(error, str):
        source = {"message": error, "type": envelope.get("type"), "code": envelope.get("code")}
    else:
        source = envelope

    parts = []
    for field in ("type", "code"):
        value = source.get(field)
        if isinstance(value, str) and re.fullmatch(r"[A-Za-z0-9_.:-]{1,80}", value.strip()):
            parts.append(f"{field}={value.strip()}")

    message = source.get("message")
    if not isinstance(message, str):
        message = source.get("detail")
    if isinstance(message, str) and message.strip():
        parts.append(f"message={message}")

    if not parts:
        return None
    return _sanitize_error_detail("; ".join(parts), api_key)


def _is_model_not_found_error(error: ModelEvalError) -> bool:
    if error.code != "gateway_http_400" or not error.safe_detail:
        return False
    return "code=model_not_found" in error.safe_detail.split("; ")


def _redact_tree(value: object, api_key: str):
    if isinstance(value, str):
        return _redact(value, api_key)
    if isinstance(value, list):
        return [_redact_tree(item, api_key) for item in value]
    if isinstance(value, dict):
        return {key: _redact_tree(item, api_key) for key, item in value.items()}
    return value


def _case_valid(case: object) -> bool:
    return isinstance(case, dict) and isinstance(case.get("case_id"), str) and isinstance(case.get("input"), dict) \
        and isinstance(case["input"].get("turns"), list) and bool(case["input"]["turns"]) \
        and isinstance(case.get("deterministic_checks"), list) and bool(case["deterministic_checks"])


def load_dataset(path: Path, *, allow_draft: bool) -> dict:
    try:
        dataset = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Could not read model evaluation dataset: {error.__class__.__name__}.") from None
    if not isinstance(dataset, dict) or not isinstance(dataset.get("cases"), list) or not dataset["cases"]:
        raise ValueError("Model evaluation dataset must contain at least one case.")
    if not all(_case_valid(case) for case in dataset["cases"]):
        raise ValueError("Dataset contains a malformed case or a case without deterministic checks.")
    if not allow_draft and any(not case.get("provenance", {}).get("approved", False) for case in dataset["cases"]):
        raise ValueError("Dataset contains unapproved draft cases; pass --allow-draft for an exploratory run.")
    case_ids = [case["case_id"] for case in dataset["cases"]]
    if len(set(case_ids)) != len(case_ids):
        raise ValueError("Dataset case IDs must be unique.")
    if dataset.get("dataset_version") == "0.2.0":
        families = [case.get("scenario_family") for case in dataset["cases"]]
        if len(set(families)) != len(families):
            raise ValueError("Dataset v0.2.0 requires one distinct scenario family per case.")
        valid_splits = {"development", "validation", "sealed_test"}
        golden_fields = {"allowed", "requested_refund_cny", "remaining_refundable_before_cny", "reason_code"}
        for case in dataset["cases"]:
            expected_output = case.get("expected_output")
            if case.get("split") not in valid_splits or not isinstance(expected_output, dict) \
                    or set(expected_output) != golden_fields:
                raise ValueError("Dataset v0.2.0 contains an invalid split or expected_output golden.")
            business_checks = {
                check.get("path", ""): check.get("expected")
                for check in case.get("deterministic_checks", [])
                if check.get("kind") == "business_rule"
            }
            if any(business_checks.get(f"$.{key}") != value for key, value in expected_output.items()):
                raise ValueError(f"Dataset golden does not match deterministic checks for {case['case_id']}.")
    return dataset


def _summarize_repetitions(results: list[dict]) -> list[dict]:
    grouped: dict[tuple[str, str], list[dict]] = {}
    for result in results:
        grouped.setdefault((result.get("requested_model", ""), result.get("case_id", "")), []).append(result)
    summary = []
    for (model, case_id), attempts in sorted(grouped.items()):
        successful = [item for item in attempts if item.get("call_status") == "success"]
        evaluable = [item for item in successful if item.get("deterministic_status") in {"passed", "failed"}]
        output_hashes = sorted({hashlib.sha256(item["raw_output"].encode("utf-8")).hexdigest()
                                for item in evaluable if isinstance(item.get("raw_output"), str)})
        pass_values = [item.get("deterministic_status") == "passed" for item in evaluable]
        if not pass_values:
            pass_consistency = "no_evaluable_attempts" if successful else "no_successful_attempts"
        elif all(pass_values):
            pass_consistency = "all_passed"
        elif not any(pass_values):
            pass_consistency = "all_failed"
        else:
            pass_consistency = "mixed"
        summary.append({
            "requested_model": model,
            "case_id": case_id,
            "attempt_count": len(attempts),
            "successful_attempt_count": len(successful),
            "deterministic_evaluated_count": len(evaluable),
            "incomplete_output_count": sum(item.get("evaluation_status") == "incomplete_output" for item in successful),
            "deterministic_pass_count": sum(pass_values),
            "deterministic_fail_count": sum(not value for value in pass_values),
            "pass_consistency": pass_consistency,
            "distinct_successful_output_count": len(output_hashes),
            "exact_output_consistency": len(output_hashes) <= 1 if output_hashes else None,
        })
    return summary


def _summarize_models(results: list[dict], case_summaries: list[dict]) -> list[dict]:
    model_names = sorted({item.get("requested_model", "") for item in results})
    summaries = []
    for model in model_names:
        attempts = [item for item in results if item.get("requested_model") == model]
        successful = [item for item in attempts if item.get("call_status") == "success"]
        evaluable = [item for item in successful if item.get("deterministic_status") in {"passed", "failed"}]
        passes = sum(item.get("deterministic_status") == "passed" for item in evaluable)
        usage_totals: dict[str, int] = {}
        for item in successful:
            usage = item.get("provider_reported_usage") or {}
            for name, value in usage.items():
                if name.endswith("_tokens") and isinstance(value, int) and not isinstance(value, bool):
                    usage_totals[name] = usage_totals.get(name, 0) + value
        cost_values = []
        for item in successful:
            usage = item.get("provider_reported_usage") or {}
            value = usage.get("total_cost", usage.get("cost"))
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                cost_values.append((float(value), usage.get("currency")))
        currencies = {currency for _, currency in cost_values}
        reported_cost = None
        if cost_values and len(currencies) == 1:
            reported_cost = {"amount": sum(value for value, _ in cost_values), "currency": next(iter(currencies))}
        stability = {
            "all_passed": 0, "all_failed": 0, "mixed": 0,
            "no_evaluable_attempts": 0, "no_successful_attempts": 0,
        }
        for item in case_summaries:
            if item["requested_model"] == model:
                stability[item["pass_consistency"]] += 1
        summaries.append({
            "requested_model": model,
            "requested_attempt_count": len(attempts),
            "successful_attempt_count": len(successful),
            "deterministic_evaluated_count": len(evaluable),
            "incomplete_output_count": sum(item.get("evaluation_status") == "incomplete_output" for item in successful),
            "gateway_error_count": sum(item.get("call_status") == "error" for item in attempts),
            "deterministic_pass_count": passes,
            "deterministic_fail_count": sum(item.get("deterministic_status") == "failed" for item in evaluable),
            "attempt_pass_rate": round(passes / len(evaluable), 6) if evaluable else None,
            "case_stability_counts": stability,
            "provider_reported_token_totals": usage_totals or None,
            "provider_reported_cost": reported_cost,
        })
    return summaries


def run_exploratory(dataset: dict, models: list[str], *, base_url: str, api_key: str = "",
                    api_keys_by_group: dict[str, str] | None = None,
                    budget_yuan: float = MAX_BUDGET_YUAN, max_output_tokens: int = DEFAULT_OUTPUT_TOKENS,
                    max_requests: int = MAX_REQUESTS, split: str = "development", repetitions: int = 1,
                    case_ids: list[str] | None = None, allow_sealed_test: bool = False,
                    continue_on_model_not_found: bool = False,
                    continue_on_gateway_timeout: bool = False,
                    continue_on_gateway_network_error: bool = False,
                    timeout_seconds: int = DEFAULT_TIMEOUT_SECONDS,
                    caller=call_chat_completion) -> dict:
    if split not in {"development", "validation", "sealed_test"}:
        raise ValueError("Select exactly one split: development, validation, or sealed_test.")
    if split == "sealed_test" and not allow_sealed_test:
        raise ValueError("sealed_test is a visible draft candidate; pass the explicit sealed-test opt-in to execute it.")
    selected_cases = [case for case in dataset["cases"] if case.get("split", "development") == split]
    if case_ids is not None:
        if len(set(case_ids)) != len(case_ids):
            raise ValueError("Case IDs must be unique.")
        cases_by_id = {case["case_id"]: case for case in dataset["cases"]}
        unknown_ids = sorted(set(case_ids) - set(cases_by_id))
        if unknown_ids:
            raise ValueError("Unknown case ID(s): " + ", ".join(unknown_ids))
        other_split_ids = sorted(case_id for case_id in case_ids if cases_by_id[case_id].get("split", "development") != split)
        if other_split_ids:
            raise ValueError(f"Selected case ID(s) must belong to split '{split}': " + ", ".join(other_split_ids))
        requested_ids = set(case_ids)
        selected_cases = [case for case in selected_cases if case["case_id"] in requested_ids]
    if not selected_cases:
        raise ValueError(f"Dataset does not contain cases in split '{split}'.")
    continue_after_gateway_error = (
        continue_on_model_not_found or continue_on_gateway_timeout or continue_on_gateway_network_error
    )
    if continue_on_model_not_found and len(selected_cases) != 1:
        raise ValueError("--continue-on-model-not-found requires exactly one selected case for a route scan.")
    if continue_after_gateway_error and repetitions != 1:
        raise ValueError("Continuation mode requires exactly one repetition.")
    validate_run_limits(models=models, case_count=len(selected_cases), max_requests=max_requests,
                        max_output_tokens=max_output_tokens, budget_yuan=budget_yuan, repetitions=repetitions,
                        timeout_seconds=timeout_seconds)
    api_keys_by_group = api_keys_by_group or {}
    missing_groups = sorted({
        model_group_for(model) for model in models
        if not api_keys_by_group.get(model_group_for(model)) and not api_key
    })
    if missing_groups:
        raise ValueError("No dedicated gateway credential is configured for model group(s): " + ", ".join(missing_groups))
    endpoint = normalize_endpoint(base_url)
    started_at = datetime.now(timezone.utc).isoformat()
    calls = []
    halted = None
    for repetition_index in range(1, repetitions + 1):
        if halted:
            break
        for case in selected_cases:
            if halted:
                break
            turns = case["input"]["turns"]
            messages = [{"role": turn["role"], "content": turn["content"]} for turn in turns]
            prompt_sha256 = hashlib.sha256(json.dumps(messages, ensure_ascii=False, sort_keys=True).encode("utf-8")).hexdigest()
            for model in models:
                if halted:
                    break
                model_group = model_group_for(model)
                model_api_key = api_keys_by_group.get(model_group) or api_key
                try:
                    if not model_api_key:
                        raise ModelEvalError("missing_credentials", f"No dedicated gateway credential is configured for model group '{model_group}'.")
                    response = caller(base_url, model_api_key, model, messages,
                                      max_output_tokens=max_output_tokens, timeout_seconds=timeout_seconds)
                    raw_output = _redact(response["content"], model_api_key)
                    checks = evaluate_response(case, raw_output)
                    output_truncated = response["finish_reason"] == "length"
                    deterministic_status = (
                        "not_evaluated" if output_truncated
                        else "passed" if checks and all(item["passed"] for item in checks)
                        else "failed"
                    )
                    call_result = {
                        "call_status": "success",
                        "case_id": case["case_id"], "scenario_family": case.get("scenario_family"),
                        "split": case.get("split"), "risk_level": case.get("risk_level"),
                        "repetition_index": repetition_index, "prompt_sha256": prompt_sha256,
                        "requested_model": model, "model_group": model_group, "returned_model": response["returned_model"],
                        "provider_request_id": response["request_id"], "finish_reason": response["finish_reason"],
                        "latency_ms": response["latency_ms"], "provider_reported_usage": response["usage"],
                        "usage_status": "available" if response["usage"] else "unavailable",
                        "billing_status": "provider_reported_only" if response["usage"] and ("cost" in response["usage"] or "total_cost" in response["usage"]) else "not_reported",
                        "raw_output": raw_output, "deterministic_checks": checks,
                        "deterministic_status": deterministic_status,
                        "evaluation_status": "incomplete_output" if output_truncated else "complete_output",
                        "human_review_status": "pending", "case_snapshot": _redact_tree(case, model_api_key),
                    }
                    if isinstance(response.get("response_diagnostics"), dict):
                        call_result["response_diagnostics"] = response["response_diagnostics"]
                    if output_truncated:
                        call_result["failure_reason"] = "output_truncated_at_token_limit"
                    calls.append(call_result)
                except (ModelEvalError, ValueError) as error:
                    code = error.code if isinstance(error, ModelEvalError) else "invalid_configuration"
                    call_result = {
                        "case_id": case["case_id"], "scenario_family": case.get("scenario_family"),
                        "split": case.get("split"), "repetition_index": repetition_index,
                        "prompt_sha256": prompt_sha256,
                        "requested_model": model, "model_group": model_group, "call_status": "error",
                        "error_code": code, "error_message": _redact(str(error), model_api_key),
                        "provider_reported_usage": None, "usage_status": "unavailable",
                        "billing_status": "not_reported", "human_review_status": "pending",
                        "case_snapshot": _redact_tree(case, model_api_key),
                    }
                    safe_detail = _sanitize_error_detail(error.safe_detail, model_api_key) \
                        if isinstance(error, ModelEvalError) else None
                    if safe_detail:
                        call_result["error_detail"] = safe_detail
                    calls.append(call_result)
                    may_continue_model_not_found = (
                        continue_on_model_not_found and isinstance(error, ModelEvalError)
                        and _is_model_not_found_error(error)
                    )
                    may_continue_timeout = (
                        continue_on_gateway_timeout and isinstance(error, ModelEvalError)
                        and error.code == "gateway_timeout"
                    )
                    may_continue_network_error = (
                        continue_on_gateway_network_error and isinstance(error, ModelEvalError)
                        and error.code == "gateway_network_error"
                    )
                    if may_continue_model_not_found or may_continue_timeout or may_continue_network_error:
                        continue
                    halted = "Stopped after first gateway/configuration error to avoid repeated charges."
                    break
    successful = [item for item in calls if item.get("call_status") == "success"]
    all_approved = all(case.get("provenance", {}).get("approved", False) for case in dataset["cases"])
    requested_calls = len(models) * len(selected_cases) * repetitions
    unique_case_summary = _summarize_repetitions(calls)
    model_summary = _summarize_models(calls, unique_case_summary)
    gateway_error_count = sum(1 for item in calls if item.get("call_status") == "error")
    evaluable = [item for item in successful if item.get("deterministic_status") in {"passed", "failed"}]
    deterministic_fail_count = sum(1 for item in evaluable if item.get("deterministic_status") == "failed")
    incomplete_output_count = sum(1 for item in successful if item.get("evaluation_status") == "incomplete_output")
    execution_complete = len(calls) == requested_calls and gateway_error_count == 0
    return {
        "report_schema_version": "0.1.0",
        "run_id": f"model-eval-{uuid.uuid4()}",
        "run_mode": "real_model_exploratory" if all_approved else "real_model_exploratory_draft",
        "provider": "openai_compatible_gateway",
        "provider_endpoint": endpoint,
        "models": models,
        "model_groups": {model: model_group_for(model) for model in models},
        "dataset_id": dataset.get("dataset_id"),
        "dataset_version": dataset.get("dataset_version"),
        "dataset_sha256": hashlib.sha256(json.dumps(dataset, ensure_ascii=False, sort_keys=True).encode("utf-8")).hexdigest(),
        "dataset_status": dataset.get("status"),
        "selected_split": split,
        "selected_case_ids": [case["case_id"] for case in selected_cases],
        "unique_case_count": len(selected_cases),
        "repetitions_per_case_model": repetitions,
        "generation_configuration": {"temperature": 0, "max_output_tokens": max_output_tokens,
                                     "timeout_seconds": timeout_seconds, "tools_enabled": False,
                                     "retries": 0, "request_order": "paired_by_case_and_repetition"},
        "benchmark_eligible": False,
        "benchmark_blockers": [
            "Cases are synthetic drafts and require domain-owner review.",
            "The sealed_test candidate is visible in the repository and is not a confidential blind holdout.",
            "Human rubric reviews, calibration, and reviewer agreement have not been recorded.",
            "This dataset covers only one synthetic refund-policy domain, not general model ability.",
            "Provider billing and underlying model routing may not be independently verifiable.",
        ],
        "human_review_status": "pending",
        "declared_budget_yuan": budget_yuan,
        "budget_control": {
            "max_requests": max_requests, "max_output_tokens_per_request": max_output_tokens,
            "timeout_seconds_per_request": timeout_seconds,
            "repetitions": repetitions, "parallelism": 1, "retries": 0, "tools_enabled": False,
            "note": "Client-side request bounds are enforced serially. The declared budget is not a hard provider billing cap; reconcile gateway account usage before claiming spend.",
        },
        "started_at": started_at,
        "completed_at": datetime.now(timezone.utc).isoformat(),
        "requested_calls": requested_calls,
        "actual_calls": len(calls),
        "execution_status": (
            "completed" if gateway_error_count == 0 else "completed_with_errors"
        ) if len(calls) == requested_calls else "partial_or_failed",
        "quality_gate_status": "passed" if execution_complete and incomplete_output_count == 0 and deterministic_fail_count == 0 else "failed",
        "gateway_error_count": gateway_error_count,
        "deterministic_evaluated_count": len(evaluable),
        "incomplete_output_count": incomplete_output_count,
        "deterministic_pass_count": sum(1 for item in evaluable if item.get("deterministic_status") == "passed"),
        "deterministic_fail_count": deterministic_fail_count,
        "per_case_model_repetition_summary": unique_case_summary,
        "per_model_summary": model_summary,
        "not_run_reason": halted,
        "results": calls,
    }


def write_report(output_dir: Path, report: dict) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    path = output_dir / f"{stamp}-{report['run_id']}.json"
    with path.open("x", encoding="utf-8", newline="\n") as output:
        json.dump(report, output, ensure_ascii=False, indent=2)
        output.write("\n")
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=Path(__file__).with_name("dataset.v0.2.0.json"))
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--models", default=",".join(DEFAULT_MODELS), help="Comma-separated exact model IDs from the gateway catalog.")
    parser.add_argument("--list-models", action="store_true", help="Read the model IDs visible to configured gateway key(s); sends no inference request.")
    parser.add_argument("--budget-yuan", type=float, default=DEFAULT_BUDGET_YUAN,
                        help="Declared run budget metadata; not a provider-side billing cap.")
    parser.add_argument("--max-requests", type=int, default=DEFAULT_REQUEST_CAP,
                        help=f"Client-side call cap (default {DEFAULT_REQUEST_CAP}, hard maximum {MAX_REQUESTS}).")
    parser.add_argument("--max-output-tokens", type=int, default=DEFAULT_OUTPUT_TOKENS,
                        help=f"Maximum generated tokens per request (default {DEFAULT_OUTPUT_TOKENS}, hard maximum {MAX_OUTPUT_TOKENS}).")
    parser.add_argument("--timeout-seconds", type=int, default=DEFAULT_TIMEOUT_SECONDS,
                        help=f"Per-request gateway timeout in seconds (1-{MAX_TIMEOUT_SECONDS}; default {DEFAULT_TIMEOUT_SECONDS}).")
    parser.add_argument("--split", choices=("development", "validation", "sealed_test"), default="development",
                        help="Run one split; development is selected by default.")
    parser.add_argument("--case-ids", help="Optional comma-separated case IDs, all of which must belong to the selected split.")
    parser.add_argument("--repetitions", type=int, default=1,
                        help=f"Serial attempts per case/model pair (1-{MAX_REPETITIONS}); repeats are not independent cases.")
    parser.add_argument("--allow-sealed-test", action="store_true",
                        help="Explicitly allow the visible sealed_test candidate split; this does not make it blind.")
    parser.add_argument("--continue-on-model-not-found", action="store_true",
                        help="During a one-case route scan, record model_not_found and continue with other models.")
    parser.add_argument("--continue-on-gateway-timeout", action="store_true",
                        help="With one repetition, record a gateway timeout and continue to the next planned case/model pair without retrying the timed-out request.")
    parser.add_argument("--continue-on-gateway-network-error", action="store_true",
                        help="With one repetition, record a gateway connection error and continue to the next planned case/model pair without retrying the failed request.")
    parser.add_argument("--allow-draft", action="store_true", help="Allow the explicitly non-benchmark draft model-evaluation dataset.")
    parser.add_argument("--execute", action="store_true", help="Send real requests to the configured gateway.")
    args = parser.parse_args(argv)
    if args.list_models and args.execute:
        print("error=--list-models is read-only and cannot be combined with --execute.", file=sys.stderr)
        return 2
    base_url = os.environ.get("MODEL_EVAL_BASE_URL") or os.environ.get("PROMPTOPS_PROVIDER_BASE_URL") or DEFAULT_BASE_URL
    if args.list_models:
        credentials = configured_catalog_credentials()
        if not credentials:
            print("error=No dedicated gateway credential is configured; set a group-specific or gateway credential locally.", file=sys.stderr)
            return 2
        try:
            for group, key in credentials:
                model_ids = call_model_catalog(base_url, key)
                print(f"group={group} model_count={len(model_ids)}")
                for model_id in model_ids:
                    print(f"  {model_id}")
            return 0
        except (ModelEvalError, ValueError, OSError) as error:
            print(f"error={error}", file=sys.stderr)
            return 1
    if not args.execute:
        print("No external requests sent. Re-run with --execute to perform the real-model pilot.", file=sys.stderr)
        return 2
    try:
        if args.output_dir is None:
            raise ValueError("--output-dir is required when executing a model evaluation.")
        dataset = load_dataset(args.dataset, allow_draft=args.allow_draft)
        models = [model.strip() for model in args.models.split(",") if model.strip()]
        if args.split == "sealed_test" and not args.allow_sealed_test:
            raise ValueError("sealed_test is a visible draft candidate; pass --allow-sealed-test only for an explicit exploratory run.")
        case_ids = [case_id.strip() for case_id in args.case_ids.split(",") if case_id.strip()] if args.case_ids else None
        split_case_ids = {case["case_id"] for case in dataset["cases"]
                          if case.get("split", "development") == args.split}
        selected_case_count = sum(case_id in split_case_ids for case_id in case_ids) if case_ids is not None else len(split_case_ids)
        if case_ids is not None and selected_case_count != len(case_ids):
            raise ValueError("Every --case-ids value must exist and belong to the selected split.")
        validate_run_limits(models=models, case_count=selected_case_count, max_requests=args.max_requests,
                            max_output_tokens=args.max_output_tokens, budget_yuan=args.budget_yuan,
                            repetitions=args.repetitions, timeout_seconds=args.timeout_seconds)
        api_key = configured_api_key()
        api_keys_by_group = {model_group_for(model): configured_api_key(model=model) for model in models}
        api_keys_by_group = {group: value for group, value in api_keys_by_group.items() if value}
        if not api_key and not api_keys_by_group:
            raise ValueError("No dedicated gateway credential is configured; set a model-group-specific key or PROMPTOPS_PROVIDER_API_KEY locally.")
        report = run_exploratory(dataset, models, base_url=base_url, api_key=api_key,
                                 api_keys_by_group=api_keys_by_group,
                                 budget_yuan=args.budget_yuan, max_output_tokens=args.max_output_tokens,
                                 max_requests=args.max_requests, split=args.split,
                                 repetitions=args.repetitions, case_ids=case_ids,
                                 allow_sealed_test=args.allow_sealed_test,
                                 continue_on_model_not_found=args.continue_on_model_not_found,
                                 continue_on_gateway_timeout=args.continue_on_gateway_timeout,
                                 continue_on_gateway_network_error=args.continue_on_gateway_network_error,
                                 timeout_seconds=args.timeout_seconds,
                                 caller=call_chat_completion)
        path = write_report(args.output_dir, report)
        print(f"report_path={path.resolve()}")
        print(f"run_id={report['run_id']} actual_calls={report['actual_calls']} gateway_errors={report['gateway_error_count']} quality_gate={report['quality_gate_status']}")
        print(f"deterministic_evaluated={report['deterministic_evaluated_count']} deterministic_pass={report['deterministic_pass_count']} deterministic_fail={report['deterministic_fail_count']} incomplete_output={report['incomplete_output_count']} benchmark_eligible=false")
        return 0 if report["quality_gate_status"] == "passed" else 1
    except (ValueError, OSError) as error:
        print(f"error={error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
