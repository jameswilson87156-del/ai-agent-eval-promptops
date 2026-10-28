import json
import os
import urllib.parse
import urllib.error
import urllib.request
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[2]
DATASET_PATH = ROOT / "backend/src/main/resources/agent-eval-dataset-v1.json"


def load_dataset():
    return json.loads(DATASET_PATH.read_text(encoding="utf-8"))


def test_dataset_has_unique_ids_schema_and_requested_distribution():
    data = load_dataset()
    cases = data["cases"]
    assert data["dataset_version"] == "1.0.2"
    assert len(cases) == 26
    assert len({case["case_id"] for case in cases}) == 26
    required = {"case_id", "title", "category", "description", "input", "expected_behavior", "assertions", "risk_level", "tags", "enabled"}
    assert all(required <= case.keys() and case["assertions"] for case in cases)
    assert {case["category"] for case in cases} == {
        "normal_output", "input_boundary", "tool_selection_arguments", "tool_failures", "security", "completion_trace_rules"
    }
    assert [sum(case["category"] == category for case in cases) for category in (
        "normal_output", "input_boundary", "tool_selection_arguments", "tool_failures", "security", "completion_trace_rules"
    )] == [6, 4, 5, 4, 3, 4]


def api_request(path, method="GET", payload=None):
    base = os.environ.get("PROMPTOPS_API_URL")
    if not base:
        pytest.skip("PROMPTOPS_API_URL is required for API integration checks")
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    headers = {"Content-Type": "application/json"} if data is not None else {}
    request = urllib.request.Request(base.rstrip("/") + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=15) as response:
        return json.loads(response.read().decode("utf-8"))


@pytest.fixture(scope="module")
def two_runs():
    return api_request("/api/agent-eval/runs", "POST"), api_request("/api/agent-eval/runs", "POST")


def test_api_report_snapshot_counts_and_execution_vs_behavior(two_runs):
    first, _ = two_runs
    assert first["report_schema_version"] == "1.1"
    assert {"run_id", "evaluator_mode", "run_mode", "provider_type", "model_name", "prompt_version", "agent_config_version", "config_fingerprint", "runtime_config_fingerprint", "version_labels_semantics", "dataset_version", "started_at", "completed_at", "duration_ms", "report_generated_at", "summary", "cases", "traces", "errors", "quality_gate"} <= first.keys()
    assert first["evaluator_mode"] == "mock"
    assert first["run_mode"] == first["provider_type"] == "mock"
    assert first["model_name"] == "deterministic-mock-agent"
    assert len(first["config_fingerprint"]) == 64
    assert len(first["runtime_config_fingerprint"]) == 64
    assert first["version_labels_semantics"] == "metadata_only"
    assert "api_key" not in first
    assert first["summary"]["provider_errors"] == 0
    assert first["state"] == "COMPLETED"
    assert len(first["cases"]) == first["summary"]["executed_cases"] == 26
    assert first["summary"]["total_cases"] == 26
    assert first["summary"]["passed"] + first["summary"]["failed"] + first["summary"]["errors"] + first["summary"]["skipped"] == 26
    assert all(case["execution_status"] == "success" for case in first["cases"])
    assert first["summary"]["failed"] > 0
    assert first["quality_gate"] == "failed"
    assert all("assertion_results" in case and "execution_steps" in case for case in first["cases"])
    assert len(first["traces"]) == sum(len(case["execution_steps"]) for case in first["cases"])
    assert all({"event_id", "case_id", "sequence", "event_type", "timestamp", "duration_ms", "input_summary", "output_summary", "status", "error"} <= event.keys() for event in first["traces"])
    assert all(case["failure_reason"] for case in first["cases"] if case["case_status"] == "failed")
    weather = next(case for case in first["cases"] if case["case_id"] == "agent-002")
    assert weather["case_status"] == "passed"
    assert [event["sequence"] for event in weather["execution_steps"]] == list(range(8))
    assert weather["execution_steps"][-1]["event_type"] == "run_completed"
    assert all(event["duration_ms"] >= 0 for event in weather["execution_steps"])
    assert weather["execution_steps"][3]["output_summary"]["arguments"]["city"] == "东京"
    timeout = next(case for case in first["cases"] if case["case_id"] == "agent-016")
    assert timeout["execution_steps"][3]["status"] == "error"
    assert timeout["execution_steps"][3]["error"] == "Mock tool behavior: timeout"
    for case_id in ("agent-020", "agent-021", "agent-022"):
        security_case = next(case for case in first["cases"] if case["case_id"] == case_id)
        assert security_case["case_status"] == "passed"
        assert security_case["actual_output"]["refused"] is True
        assert security_case["actual_output"]["tool_calls"] == []
        assert any(result["path"] == "refused" and result["passed"] for result in security_case["assertions"])
    no_fabrication = next(case for case in first["cases"] if case["case_id"] == "agent-026")
    assert no_fabrication["case_status"] == "passed"
    assert any(result["operator"] == "not_contains" and result["passed"] for result in no_fabrication["assertions"])


def test_repeated_run_is_deterministic_and_history_report_matches(two_runs):
    first, second = two_runs
    assert first["run_id"] != second["run_id"]
    stable = lambda run: [(case["case_id"], case["case_status"], case["actual_output"]) for case in run["cases"]]
    assert stable(first) == stable(second)
    saved = api_request(f"/api/agent-eval/runs/{first['run_id']}")
    exported = api_request(f"/api/agent-eval/runs/{first['run_id']}/report")
    assert saved == first == exported
    history = api_request("/api/agent-eval/runs")
    assert {first["run_id"], second["run_id"]} <= {run["run_id"] for run in history}


def test_failure_case_can_be_reproduced():
    first = api_request("/api/agent-eval/runs", "POST")
    second = api_request("/api/agent-eval/runs", "POST")
    case_a = next(case for case in first["cases"] if case["case_id"] == "agent-014")
    case_b = next(case for case in second["cases"] if case["case_id"] == "agent-014")
    assert case_a["case_status"] == "failed"
    assert case_a["failure_reason"]
    assert case_a["actual_output"]["tool_calls"][0]["name"] == "web_search"
    assert case_a["actual_output"] == case_b["actual_output"]
    repeated_a = next(case for case in first["cases"] if case["case_id"] == "agent-015")
    repeated_b = next(case for case in second["cases"] if case["case_id"] == "agent-015")
    assert repeated_a["execution_status"] == "success"
    assert repeated_a["case_status"] == "failed"
    assert repeated_a["failure_reason"]
    assert len(repeated_a["actual_output"]["tool_calls"]) == 2
    assert repeated_a["actual_output"] == repeated_b["actual_output"]


def test_dataset_api_matches_committed_definition():
    api_dataset = api_request("/api/agent-eval/dataset")
    local = load_dataset()
    assert api_dataset["dataset_version"] == local["dataset_version"]
    assert [case["case_id"] for case in api_dataset["cases"]] == [case["case_id"] for case in local["cases"]]


def test_provider_status_is_safe_and_does_not_return_credentials():
    status = api_request("/api/agent-eval/provider/status")
    encoded = json.dumps(status).lower()
    assert status["default_provider_type"] == "mock"
    assert status["real_provider_requires_explicit_selection"] is True
    assert status["model_credentials_returned"] is False
    assert "api_key" not in encoded
    assert "authorization" not in encoded
    provider = next(value for value in status["providers"] if value["provider_type"] == "openai_compatible")
    assert "credentials_configured" in provider
    assert "model_name" in provider


def test_comparison_uses_two_persisted_runs_and_labels_mock_metadata_only():
    baseline = api_request("/api/agent-eval/runs", "POST", {
        "provider_type": "mock", "prompt_version": "mock-prompt-a", "agent_config_version": "mock-agent-v1"
    })
    candidate = api_request("/api/agent-eval/runs", "POST", {
        "provider_type": "mock", "prompt_version": "mock-prompt-b", "agent_config_version": "mock-agent-v1"
    })
    query = urllib.parse.urlencode({
        "baseline_run_id": baseline["run_id"], "candidate_run_id": candidate["run_id"]
    })
    comparison = api_request(f"/api/agent-eval/comparisons?{query}")
    assert comparison["baseline"]["run_id"] == baseline["run_id"]
    assert comparison["candidate"]["run_id"] == candidate["run_id"]
    assert comparison["comparison_semantics"] == "mock_metadata_only_comparison"
    assert comparison["is_version_regression"] is False
    assert comparison["configuration_changed"] is False
    assert comparison["configuration_labels_changed"] is True
    assert comparison["run_fingerprint_changed"] is True
    assert comparison["summary"]["persistent_failures"] == 2
    assert len(comparison["case_changes"]) == 26
    assert any(change["field"] == "prompt_version" for change in comparison["metadata_changes"])
    assert len(comparison["category_changes"]) == 6
