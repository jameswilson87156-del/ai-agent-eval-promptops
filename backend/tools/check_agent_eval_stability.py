#!/usr/bin/env python3
"""Bounded local Mock API stability and latency sampler for Agent Eval."""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
import hashlib
import ipaddress
import json
import math
from pathlib import Path
import statistics
import sys
import time
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


BACKEND_ROOT = Path(__file__).resolve().parents[1]
DATASET_PATH = BACKEND_ROOT / "src/main/resources/agent-eval-dataset-v1.json"
MAX_ITERATIONS = 100
MAX_CONCURRENCY = 16
MAX_WARMUP_RUNS = 3


def percentile(values: list[float], percentile_value: float) -> float:
    ordered = sorted(values)
    rank = max(1, math.ceil(percentile_value * len(ordered)))
    return ordered[rank - 1]


def normalize_trace_assertion_value(value):
    if isinstance(value, list):
        return [normalize_trace_assertion_value(item) for item in value]
    if isinstance(value, dict):
        volatile = {"event_id", "timestamp", "duration_ms"} if "event_type" in value else set()
        return {
            key: normalize_trace_assertion_value(item)
            for key, item in value.items()
            if key not in volatile
        }
    return value


def stable_signature(report: dict) -> str:
    cases = [
        {
            "case_id": case.get("case_id"),
            "execution_status": case.get("execution_status"),
            "case_status": case.get("case_status"),
            "actual_output": case.get("actual_output"),
            "assertion_results": [
                {
                    **assertion,
                    "actual": normalize_trace_assertion_value(assertion.get("actual")),
                }
                if assertion.get("path") == "trace" else assertion
                for assertion in case.get("assertion_results", [])
            ],
            "failure_reason": case.get("failure_reason"),
        }
        for case in report.get("cases", [])
    ]
    encoded = json.dumps(cases, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def validate_loopback_url(base_url: str) -> str:
    parsed = urlsplit(base_url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise ValueError("base URL must be an HTTP(S) URL with a hostname")
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ValueError("base URL must not contain credentials, query parameters, or fragments")
    try:
        parsed.port
    except ValueError as error:
        raise ValueError("base URL contains an invalid port") from error
    if parsed.path not in {"", "/"}:
        raise ValueError("base URL must be an origin without an API path")
    host = parsed.hostname.lower()
    try:
        is_loopback = ipaddress.ip_address(host).is_loopback
    except ValueError:
        is_loopback = host == "localhost"
    if not is_loopback:
        raise ValueError("this stability sampler only targets localhost or a loopback IP")
    return base_url.rstrip("/")


def run_once(
    base_url: str,
    expected_dataset_version: str,
    expected_case_ids: list[str],
    timeout_seconds: float,
    index: int,
) -> dict:
    payload = {
        "provider_type": "mock",
        "prompt_version": "local-mock-stability-check",
        "agent_config_version": "deterministic-mock-stability-check",
    }
    request = Request(
        f"{base_url}/api/agent-eval/runs",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    started = time.perf_counter()
    try:
        with urlopen(request, timeout=timeout_seconds) as response:
            status_code = response.status
            response_body = response.read()
        elapsed_ms = (time.perf_counter() - started) * 1000
        report = json.loads(response_body.decode("utf-8"))
        if not isinstance(report, dict):
            return {
                "index": index,
                "success": False,
                "http_status": status_code,
                "duration_ms": round(elapsed_ms, 3),
                "run_id": None,
                "error": "response was not a JSON object",
            }
        cases = report.get("cases", [])
        summary = report.get("summary", {})
        if not isinstance(cases, list) or not isinstance(summary, dict):
            return {
                "index": index,
                "success": False,
                "http_status": status_code,
                "duration_ms": round(elapsed_ms, 3),
                "run_id": report.get("run_id"),
                "error": "response did not contain a valid cases list and summary object",
            }
        case_ids = [case.get("case_id") for case in cases]
        execution_errors = sum(case.get("execution_status") != "success" for case in cases)
        behavior_passes = sum(case.get("case_status") == "passed" for case in cases)
        behavior_failures = sum(case.get("case_status") == "failed" for case in cases)
        behavior_statuses_valid = all(case.get("case_status") in {"passed", "failed"} for case in cases)
        invalid_response = (
            status_code != 200
            or report.get("state") != "COMPLETED"
            or not isinstance(report.get("run_id"), str)
            or not report.get("run_id", "").strip()
            or report.get("evaluator_mode") != "mock"
            or report.get("run_mode") != "mock"
            or report.get("provider_type") != "mock"
            or report.get("dataset_version") != expected_dataset_version
            or case_ids != expected_case_ids
            or summary.get("executed_cases") != len(expected_case_ids)
            or summary.get("passed") != behavior_passes
            or summary.get("failed") != behavior_failures
            or summary.get("errors") != 0
            or summary.get("provider_errors", 0) != 0
            or summary.get("evaluator_errors", 0) != 0
            or execution_errors != 0
            or not behavior_statuses_valid
            or report.get("quality_gate") not in {"passed", "failed"}
        )
        return {
            "index": index,
            "success": not invalid_response,
            "http_status": status_code,
            "duration_ms": round(elapsed_ms, 3),
            "run_id": report.get("run_id"),
            "state": report.get("state"),
            "run_mode": report.get("run_mode"),
            "dataset_version": report.get("dataset_version"),
            "case_count": len(cases),
            "behavior_passed": summary.get("passed", 0),
            "behavior_failed": behavior_failures,
            "behavior_statuses_valid": behavior_statuses_valid,
            "execution_errors": execution_errors,
            "provider_errors": summary.get("provider_errors", 0),
            "evaluator_errors": summary.get("evaluator_errors", 0),
            "skipped": summary.get("skipped", 0),
            "quality_gate": report.get("quality_gate"),
            "signature": stable_signature(report),
            "error": None if not invalid_response else "response did not satisfy the Mock run contract",
        }
    except HTTPError as error:
        elapsed_ms = (time.perf_counter() - started) * 1000
        return {
            "index": index,
            "success": False,
            "http_status": error.code,
            "duration_ms": round(elapsed_ms, 3),
            "run_id": None,
            "error": f"HTTP {error.code}",
        }
    except Exception as error:
        elapsed_ms = (time.perf_counter() - started) * 1000
        return {
            "index": index,
            "success": False,
            "http_status": None,
            "duration_ms": round(elapsed_ms, 3),
            "run_id": None,
            "error": type(error).__name__,
        }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run bounded deterministic Mock executions against a local Agent Eval API."
    )
    parser.add_argument("--base-url", required=True, help="local API origin, for example http://127.0.0.1:18080")
    parser.add_argument("--iterations", type=int, default=12, help=f"measured runs, 1-{MAX_ITERATIONS} (default: 12)")
    parser.add_argument("--concurrency", type=int, default=3, help=f"parallel HTTP requests, 1-{MAX_CONCURRENCY} (default: 3)")
    parser.add_argument("--warmup-runs", type=int, default=1, help=f"sequential warm-up runs, 0-{MAX_WARMUP_RUNS} (default: 1)")
    parser.add_argument("--timeout-seconds", type=float, default=60.0, help="per-request timeout (default: 60)")
    parser.add_argument("--max-p95-ms", type=float, help="optional local HTTP p95 budget; omitted means observation only")
    parser.add_argument("--output", required=True, help="new JSON output path; an existing file is never overwritten")
    args = parser.parse_args()
    if not 1 <= args.iterations <= MAX_ITERATIONS:
        parser.error(f"--iterations must be between 1 and {MAX_ITERATIONS}")
    if not 1 <= args.concurrency <= MAX_CONCURRENCY:
        parser.error(f"--concurrency must be between 1 and {MAX_CONCURRENCY}")
    if not 0 <= args.warmup_runs <= MAX_WARMUP_RUNS:
        parser.error(f"--warmup-runs must be between 0 and {MAX_WARMUP_RUNS}")
    if not math.isfinite(args.timeout_seconds) or args.timeout_seconds <= 0:
        parser.error("--timeout-seconds must be greater than zero")
    if args.max_p95_ms is not None and (not math.isfinite(args.max_p95_ms) or args.max_p95_ms <= 0):
        parser.error("--max-p95-ms must be greater than zero")
    return args


def main() -> int:
    args = parse_args()
    try:
        base_url = validate_loopback_url(args.base_url)
    except ValueError as error:
        print(f"Invalid local target: {error}", file=sys.stderr)
        return 2

    output_path = Path(args.output).expanduser().resolve()
    if output_path.exists():
        print(f"Refusing to overwrite existing output: {output_path}", file=sys.stderr)
        return 2

    try:
        dataset = json.loads(DATASET_PATH.read_text(encoding="utf-8"))
        if not isinstance(dataset, dict) or not isinstance(dataset.get("cases"), list):
            raise ValueError("dataset must be a JSON object containing a cases list")
        if not all(isinstance(case, dict) for case in dataset["cases"]):
            raise ValueError("dataset cases must be JSON objects")
        expected_case_ids = [
            case["case_id"] for case in dataset["cases"]
            if case.get("enabled", True)
        ]
        if not expected_case_ids or len(set(expected_case_ids)) != len(expected_case_ids):
            raise ValueError("enabled dataset case IDs must be non-empty and unique")
    except (OSError, KeyError, TypeError, ValueError, json.JSONDecodeError) as error:
        print(f"Cannot load the committed evaluation dataset: {type(error).__name__}", file=sys.stderr)
        return 2

    warmups = [
        run_once(base_url, dataset.get("dataset_version"), expected_case_ids,
                 args.timeout_seconds, index=-(args.warmup_runs - offset))
        for offset in range(args.warmup_runs)
    ]
    warmups_valid = all(item["success"] for item in warmups)
    measured_started = time.perf_counter()
    results = []
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = [
            pool.submit(run_once, base_url, dataset.get("dataset_version"), expected_case_ids,
                        args.timeout_seconds, index)
            for index in range(1, args.iterations + 1)
        ]
        for future in as_completed(futures):
            results.append(future.result())
    measured_wall_seconds = time.perf_counter() - measured_started
    results.sort(key=lambda item: item["index"])

    successful = [item for item in results if item["success"]]
    valid_runs = [item for item in warmups + successful if item["success"]]
    durations = [item["duration_ms"] for item in successful]
    unique_run_ids = {item["run_id"] for item in valid_runs}
    signatures = {item["signature"] for item in valid_runs}
    behavior_failure_counts = {item["behavior_failed"] for item in valid_runs}
    quality_gates = {item["quality_gate"] for item in valid_runs}
    all_valid = len(successful) == args.iterations and warmups_valid
    outputs_stable = len(signatures) == 1 if successful else False
    run_ids_unique = len(unique_run_ids) == len(valid_runs) and len(valid_runs) == args.iterations + args.warmup_runs
    behavior_counts_stable = len(behavior_failure_counts) <= 1
    quality_gate_stable = len(quality_gates) <= 1
    latency_p95 = percentile(durations, 0.95) if durations else None
    latency_budget_met = (
        None if args.max_p95_ms is None or latency_p95 is None else latency_p95 <= args.max_p95_ms
    )
    stability_passed = (
        all_valid
        and outputs_stable
        and run_ids_unique
        and behavior_counts_stable
        and quality_gate_stable
        and (latency_budget_met is not False)
    )
    case_count = len(expected_case_ids)
    result = {
        "schema_version": "1.0",
        "created_at": datetime.now(timezone.utc).isoformat(),
        "target": "loopback Agent Eval HTTP API with deterministic Mock provider",
        "measurement_scope": "end-to-end local HTTP request plus Mock evaluator; excludes real model, external tools, browser rendering, and production infrastructure",
        "dataset_version": dataset.get("dataset_version"),
        "expected_case_count": case_count,
        "provider_type": "mock",
        "warmup_runs": warmups,
        "configuration": {
            "iterations": args.iterations,
            "concurrency": args.concurrency,
            "timeout_seconds": args.timeout_seconds,
            "latency_budget_p95_ms": args.max_p95_ms,
        },
        "summary": {
            "status": "passed" if stability_passed else "failed",
            "stability_status": "passed" if all_valid and outputs_stable and run_ids_unique and behavior_counts_stable and quality_gate_stable else "failed",
            "latency_status": "not_measured" if not durations else ("budgeted" if args.max_p95_ms is not None else "observed_only"),
            "measured_runs": args.iterations,
            "successful_runs": len(successful),
            "warmup_runs_valid": warmups_valid,
            "successful_warmup_runs": sum(item["success"] for item in warmups),
            "failed_requests": args.iterations - len(successful),
            "unique_run_ids_including_warmups": len(unique_run_ids),
            "outputs_deterministic": outputs_stable,
            "run_ids_unique": run_ids_unique,
            "behavior_failure_counts_stable": behavior_counts_stable,
            "quality_gates_stable": quality_gate_stable,
            "agent_behavior_failures_per_run": next(iter(behavior_failure_counts)) if len(behavior_failure_counts) == 1 else None,
            "quality_gate_values": sorted(str(value) for value in quality_gates),
            "evaluated_cases": sum(item.get("case_count", 0) for item in successful),
            "measured_wall_seconds": round(measured_wall_seconds, 3),
            "runs_per_second": round(len(successful) / measured_wall_seconds, 3) if measured_wall_seconds else None,
            "cases_per_second": round(sum(item.get("case_count", 0) for item in successful) / measured_wall_seconds, 3) if measured_wall_seconds else None,
            "latency_ms": {
                "percentile_method": "nearest_rank",
                "min": round(min(durations), 3) if durations else None,
                "median": round(statistics.median(durations), 3) if durations else None,
                "p95": round(latency_p95, 3) if latency_p95 is not None else None,
                "max": round(max(durations), 3) if durations else None,
                "budget_met": latency_budget_met,
            },
        },
        "runs": [
            {key: value for key, value in item.items() if key != "signature"}
            for item in results
        ],
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        with output_path.open("x", encoding="utf-8", newline="\n") as handle:
            json.dump(result, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
    except FileExistsError:
        print(f"Refusing to overwrite existing output: {output_path}", file=sys.stderr)
        return 2
    except OSError as error:
        print(f"Could not write output file: {type(error).__name__}", file=sys.stderr)
        return 2

    print(json.dumps({
        "status": result["summary"]["status"],
        "stability_status": result["summary"]["stability_status"],
        "latency_status": result["summary"]["latency_status"],
        "runs": args.iterations,
        "successful_runs": len(successful),
        "successful_warmup_runs": sum(item["success"] for item in warmups),
        "failed_requests": args.iterations - len(successful),
        "p95_ms": result["summary"]["latency_ms"]["p95"],
        "outputs_deterministic": outputs_stable,
        "evidence": str(output_path),
    }, ensure_ascii=False))
    return 0 if stability_passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
