import copy

import pytest

from summarize_runs import build_combined_summary


def report(run_id, case_ids, models, results):
    return {
        "run_id": run_id,
        "dataset_id": "synthetic-refund",
        "dataset_version": "0.2.0",
        "dataset_sha256": "same-hash",
        "selected_split": "development",
        "selected_case_ids": case_ids,
        "models": models,
        "repetitions_per_case_model": 1,
        "results": results,
    }


def result(case_id, model, call_status, deterministic_status=None, output=None):
    return {
        "case_id": case_id,
        "requested_model": model,
        "repetition_index": 1,
        "call_status": call_status,
        "evaluation_status": "complete_output" if call_status == "success" else None,
        "deterministic_status": deterministic_status,
        "raw_output": output,
        "error_code": "gateway_network_error" if call_status == "error" else None,
        "provider_reported_usage": {"prompt_tokens": 10, "completion_tokens": 2, "total_tokens": 12} if call_status == "success" else None,
        "billing_status": "not_reported",
        "latency_ms": 10 if call_status == "success" else None,
        "deterministic_checks": [],
    }


def test_merged_reports_keep_planned_coverage_and_do_not_count_retry_twice():
    model_a = "model-a"
    failed_attempt = report("run-1", ["case-1", "case-2"], [model_a], [
        result("case-1", model_a, "success", "passed", '{"ok":true}'),
        result("case-2", model_a, "error"),
    ])
    retry = report("run-2", ["case-2"], [model_a], [
        result("case-2", model_a, "success", "failed", '{"ok":false}'),
    ])

    merged = build_combined_summary([failed_attempt, retry])

    assert merged["completion_status"] == "complete"
    assert merged["planned_unique_pairs"] == 2
    assert merged["evaluated_unique_pairs"] == 2
    assert merged["deterministic_pass_pairs"] == 1
    assert merged["deterministic_fail_pairs"] == 1
    assert merged["actual_request_attempts"] == 3
    assert merged["unique_pairs_with_attempts"] == 2
    assert merged["duplicate_pair_attempts"] == 1
    assert merged["gateway_error_attempts"] == 1
    assert merged["per_model_summary"][0]["provider_reported_usage"]["total_tokens"] == 24
    assert merged["benchmark_eligible"] is False
    assert "raw_output" not in merged["attempts"][0]


def test_merge_refuses_different_dataset_identity():
    first = report("run-1", ["case-1"], ["model-a"], [result("case-1", "model-a", "success", "passed")])
    second = copy.deepcopy(first)
    second["dataset_sha256"] = "different-hash"

    with pytest.raises(ValueError, match="different datasets or splits"):
        build_combined_summary([first, second])
