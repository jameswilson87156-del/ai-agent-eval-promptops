"""Merge partial model-evaluation reports without double-counting retries."""

from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
from typing import Any


IDENTITY_FIELDS = ("dataset_id", "dataset_version", "dataset_sha256", "selected_split")


def _safe_scalar(value: Any) -> Any:
    if value is None or isinstance(value, (bool, int, float)):
        return value
    if isinstance(value, str):
        return value[:200]
    return "[omitted]"


def _percentile(values: list[float], quantile: float) -> float | None:
    if not values:
        return None
    ordered = sorted(values)
    rank = max(1, int((len(ordered) * quantile) + 0.999999))
    return round(ordered[min(rank, len(ordered)) - 1], 3)


def _usage(result: dict) -> dict[str, int] | None:
    usage = result.get("provider_reported_usage")
    if not isinstance(usage, dict):
        return None
    return {
        key: int(usage.get(key) or 0)
        for key in ("prompt_tokens", "completion_tokens", "total_tokens")
    }


def _failed_checks(result: dict) -> list[dict[str, Any]]:
    checks = result.get("deterministic_checks")
    if not isinstance(checks, list):
        return []
    return [
        {
            "check_id": str(check.get("check_id", "unknown")),
            "expected": _safe_scalar(check.get("expected")),
            "actual": _safe_scalar(check.get("actual")),
        }
        for check in checks
        if isinstance(check, dict) and check.get("passed") is False
    ]


def _attempt_record(report: dict, result: dict) -> dict[str, Any]:
    raw_output = result.get("raw_output")
    output_hash = (
        hashlib.sha256(raw_output.encode("utf-8")).hexdigest()
        if isinstance(raw_output, str) and raw_output
        else None
    )
    return {
        "run_id": report.get("run_id"),
        "case_id": result.get("case_id"),
        "requested_model": result.get("requested_model"),
        "repetition_index": result.get("repetition_index"),
        "call_status": result.get("call_status"),
        "evaluation_status": result.get("evaluation_status"),
        "deterministic_status": result.get("deterministic_status"),
        "error_code": result.get("error_code"),
        "latency_ms": result.get("latency_ms"),
        "usage": _usage(result),
        "billing_status": result.get("billing_status"),
        "output_sha256": output_hash,
        "failed_checks": _failed_checks(result),
    }


def build_combined_summary(reports: list[dict]) -> dict[str, Any]:
    if not reports:
        raise ValueError("At least one model-evaluation report is required.")
    identity = {field: reports[0].get(field) for field in IDENTITY_FIELDS}
    if any(any(report.get(field) != identity[field] for field in IDENTITY_FIELDS) for report in reports):
        raise ValueError("Cannot merge reports from different datasets or splits.")

    expected_pairs: set[tuple[str, str, int]] = set()
    attempts_by_pair: dict[tuple[str, str, int], list[dict[str, Any]]] = defaultdict(list)
    all_attempts: list[dict[str, Any]] = []
    model_order: list[str] = []
    for report in reports:
        selected_cases = report.get("selected_case_ids") or []
        selected_models = report.get("models") or []
        if isinstance(selected_models, list):
            selected_models = [
                item if isinstance(item, str) else item.get("requested_model", item.get("model"))
                for item in selected_models
            ]
        for model in selected_models:
            if model and model not in model_order:
                model_order.append(str(model))
            for case_id in selected_cases:
                for repetition in range(1, int(report.get("repetitions_per_case_model", 1)) + 1):
                    expected_pairs.add((str(case_id), str(model), repetition))
        for result in report.get("results") or []:
            record = _attempt_record(report, result)
            key = (
                str(result.get("case_id")),
                str(result.get("requested_model")),
                int(result.get("repetition_index") or 1),
            )
            attempts_by_pair[key].append(record)
            all_attempts.append(record)

    pairs = []
    for case_id, model, repetition in sorted(attempts_by_pair):
        attempts = attempts_by_pair[(case_id, model, repetition)]
        completed = [
            attempt for attempt in attempts
            if attempt["call_status"] == "success"
            and attempt["evaluation_status"] == "complete_output"
            and attempt["deterministic_status"] in {"passed", "failed"}
        ]
        selected = completed[-1] if completed else attempts[-1]
        hashes = sorted({attempt["output_sha256"] for attempt in completed if attempt["output_sha256"]})
        pairs.append(
            {
                "case_id": case_id,
                "requested_model": model,
                "repetition_index": repetition,
                "pair_status": "evaluated" if completed else "unresolved",
                "deterministic_status": selected["deterministic_status"] if completed else None,
                "selected_run_id": selected["run_id"],
                "attempt_count": len(attempts),
                "gateway_error_attempts": sum(item["call_status"] != "success" for item in attempts),
                "output_consistent_across_successes": len(hashes) <= 1,
                "failed_checks": selected["failed_checks"] if completed else [],
            }
        )

    per_model = []
    for model in model_order:
        model_pairs = [pair for pair in pairs if pair["requested_model"] == model]
        evaluated = [pair for pair in model_pairs if pair["pair_status"] == "evaluated"]
        model_attempts = [attempt for attempt in all_attempts if attempt["requested_model"] == model]
        model_usage = [attempt["usage"] for attempt in model_attempts if attempt["usage"]]
        latencies = [
            float(attempt["latency_ms"])
            for attempt in model_attempts
            if attempt["call_status"] == "success" and attempt["latency_ms"] is not None
        ]
        per_model.append(
            {
                "model": model,
                "planned_pairs": len({pair for pair in expected_pairs if pair[1] == model}),
                "evaluated_pairs": len(evaluated),
                "passed_pairs": sum(pair["deterministic_status"] == "passed" for pair in evaluated),
                "failed_pairs": sum(pair["deterministic_status"] == "failed" for pair in evaluated),
                "unresolved_pairs": len(model_pairs) - len(evaluated),
                "gateway_error_attempts": sum(item["call_status"] != "success" for item in model_attempts),
                "provider_reported_usage": {
                    key: sum(usage[key] for usage in model_usage)
                    for key in ("prompt_tokens", "completion_tokens", "total_tokens")
                },
                "billing_status": "provider_did_not_report_cost",
                "latency_ms": {
                    "n": len(latencies),
                    "p50_nearest_rank": _percentile(latencies, 0.50),
                    "p95_nearest_rank": _percentile(latencies, 0.95),
                },
            }
        )

    expected_count = len(expected_pairs)
    evaluated_count = sum(pair["pair_status"] == "evaluated" for pair in pairs)
    passing_count = sum(pair["deterministic_status"] == "passed" for pair in pairs)
    failing_count = sum(pair["deterministic_status"] == "failed" for pair in pairs)
    unresolved_count = sum(pair["pair_status"] == "unresolved" for pair in pairs)
    return {
        "summary_schema_version": "1.0.0",
        "run_mode": "real_model_exploratory_draft",
        **identity,
        "benchmark_eligible": False,
        "human_review_status": "pending",
        "billing_status": "provider_did_not_report_cost",
        "source_run_ids": [report.get("run_id") for report in reports],
        "requested_models": model_order,
        "planned_unique_pairs": expected_count,
        "unique_pairs_with_attempts": len(pairs),
        "evaluated_unique_pairs": evaluated_count,
        "deterministic_pass_pairs": passing_count,
        "deterministic_fail_pairs": failing_count,
        "unresolved_unique_pairs": unresolved_count,
        "actual_request_attempts": len(all_attempts),
        "gateway_error_attempts": sum(item["call_status"] != "success" for item in all_attempts),
        "duplicate_pair_attempts": len(all_attempts) - len(pairs),
        "completion_status": "complete" if expected_count == evaluated_count and unresolved_count == 0 else "partial",
        "quality_gate_status": "passed" if expected_count == evaluated_count and failing_count == 0 and unresolved_count == 0 else "failed",
        "per_model_summary": per_model,
        "pairs": pairs,
        "attempts": all_attempts,
        "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports", nargs="+", required=True, help="Input run-scoped JSON reports")
    parser.add_argument("--output", required=True, help="New output JSON path")
    args = parser.parse_args()
    output_path = Path(args.output)
    if output_path.exists():
        parser.error(f"Refusing to overwrite existing output: {output_path}")
    try:
        reports = [json.loads(Path(path).read_text(encoding="utf-8")) for path in args.reports]
        summary = build_combined_summary(reports)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as error:
        print(f"model eval report summary failed: {error}")
        return 2
    print(f"summary_path={output_path}")
    print(
        f"status={summary['completion_status']} evaluated={summary['evaluated_unique_pairs']}"
        f"/{summary['planned_unique_pairs']} pass={summary['deterministic_pass_pairs']}"
        f" fail={summary['deterministic_fail_pairs']} unresolved={summary['unresolved_unique_pairs']}"
        f" attempts={summary['actual_request_attempts']} gateway_errors={summary['gateway_error_attempts']}"
        f" benchmark_eligible={str(summary['benchmark_eligible']).lower()}"
    )
    return 0 if summary["completion_status"] == "complete" else 1


if __name__ == "__main__":
    raise SystemExit(main())
