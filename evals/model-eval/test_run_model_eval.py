import importlib.util
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Thread

import pytest


MODULE_PATH = Path(__file__).with_name("run_model_eval.py")
SPEC = importlib.util.spec_from_file_location("run_model_eval", MODULE_PATH)
runner = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(runner)
GENERATOR_PATH = Path(__file__).with_name("generate_dataset.py")
GENERATOR_SPEC = importlib.util.spec_from_file_location("generate_dataset", GENERATOR_PATH)
dataset_generator = importlib.util.module_from_spec(GENERATOR_SPEC)
GENERATOR_SPEC.loader.exec_module(dataset_generator)


def case_with_checks(checks):
    return {"case_id": "model-001", "deterministic_checks": checks}


def test_model_catalog_request_lists_only_ids_visible_to_supplied_key():
    observed = {}

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            observed["path"] = self.path
            observed["authorization"] = self.headers.get("Authorization")
            payload = json.dumps({"object": "list", "data": [
                {"id": "glm-5.3-flash", "object": "model"},
                {"id": "deepseek-v4.1-flash", "object": "model"},
            ]}).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        def log_message(self, *_args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        model_ids = runner.call_model_catalog(
            f"http://127.0.0.1:{server.server_port}/v1", "catalog-secret-never-print",
        )
    finally:
        server.shutdown()
        thread.join(timeout=2)
        server.server_close()

    assert observed["path"] == "/v1/models"
    assert observed["authorization"] == "Bearer catalog-secret-never-print"
    assert model_ids == ["deepseek-v4.1-flash", "glm-5.3-flash"]
    assert "catalog-secret-never-print" not in json.dumps(model_ids)


def test_list_models_cli_queries_each_configured_group_without_printing_keys(monkeypatch, capsys):
    monkeypatch.setenv("PROMPTOPS_PROVIDER_API_KEY_GUOMODEL", "guomodel-local-secret")
    monkeypatch.setenv("PROMPTOPS_PROVIDER_API_KEY_GEMINI", "gemini-local-secret")
    monkeypatch.delenv("PROMPTOPS_PROVIDER_API_KEY", raising=False)
    monkeypatch.setenv("MODEL_EVAL_BASE_URL", "https://gateway.example/v1")
    queried = []

    def catalog(base_url, api_key):
        queried.append((base_url, api_key))
        return ["glm-5.3-flash"] if api_key.startswith("guomodel") else ["gemini-3.8-flash-low"]

    monkeypatch.setattr(runner, "call_model_catalog", catalog)

    exit_code = runner.main(["--list-models"])
    output = capsys.readouterr().out

    assert exit_code == 0
    assert queried == [
        ("https://gateway.example/v1", "guomodel-local-secret"),
        ("https://gateway.example/v1", "gemini-local-secret"),
    ]
    assert "group=guomodel" in output and "glm-5.3-flash" in output
    assert "group=gemini" in output and "gemini-3.8-flash-low" in output
    assert "local-secret" not in output


def test_list_models_cli_uses_a_single_generic_key_once(monkeypatch, capsys):
    monkeypatch.delenv("PROMPTOPS_PROVIDER_API_KEY_GUOMODEL", raising=False)
    monkeypatch.delenv("PROMPTOPS_PROVIDER_API_KEY_GEMINI", raising=False)
    monkeypatch.setenv("PROMPTOPS_PROVIDER_API_KEY", "universal-local-secret")
    monkeypatch.setenv("MODEL_EVAL_BASE_URL", "https://gateway.example/v1")
    queried = []
    monkeypatch.setattr(runner, "call_model_catalog", lambda base_url, api_key: queried.append((base_url, api_key)) or ["visible-model"])

    exit_code = runner.main(["--list-models"])
    output = capsys.readouterr().out

    assert exit_code == 0
    assert queried == [("https://gateway.example/v1", "universal-local-secret")]
    assert "group=gateway-visible" in output and "visible-model" in output
    assert "universal-local-secret" not in output


def test_evaluates_json_schema_and_business_rules_without_scoring_human_dimensions():
    case = case_with_checks([
        {"check_id": "schema", "kind": "json_schema", "expected": {
            "required": {"allowed": "boolean", "remaining_refundable_cny": "number"}
        }},
        {"check_id": "allowed", "kind": "business_rule", "path": "$.allowed", "expected": True},
        {"check_id": "remaining", "kind": "business_rule", "path": "$.remaining_refundable_cny", "expected": 85},
    ])

    checks = runner.evaluate_response(case, '{"allowed":true,"remaining_refundable_cny":85}')

    assert [item["passed"] for item in checks] == [True, True, True]
    assert all("score" not in item for item in checks)


def test_malformed_json_fails_checks_closed_and_retains_safe_output():
    case = case_with_checks([
        {"check_id": "schema", "kind": "json_schema", "expected": {"required": {"allowed": "boolean"}}},
        {"check_id": "marker", "kind": "not_contains", "expected": "SYNTHETIC-CANARY"},
    ])

    checks = runner.evaluate_response(case, "not-json SYNTHETIC-CANARY")

    assert [item["passed"] for item in checks] == [False, False]
    assert checks[1]["actual"] is True


def test_chat_completion_uses_bounded_openai_request_and_captures_usage_without_key_leak():
    observed = {"bodies": []}

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            observed["authorization"] = self.headers.get("Authorization")
            observed["bodies"].append(json.loads(self.rfile.read(int(self.headers["Content-Length"]))))
            payload = json.dumps({
                "id": "req-local-1",
                "model": "glm-5.3-flash",
                "choices": [{"message": {
                    "content": "{\"allowed\":true}",
                    "reasoning_content": "synthetic private reasoning must not be retained",
                }}],
                "usage": {"prompt_tokens": 17, "completion_tokens": 5, "total_tokens": 22},
            }).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("X-Request-Id", "req-local-1")
            self.end_headers()
            self.wfile.write(payload)

        def log_message(self, *_args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        result = runner.call_chat_completion(
            f"http://127.0.0.1:{server.server_port}/v1", "test-secret-do-not-report",
            "glm-5.3-flash", [{"role": "user", "content": "hello"}], max_output_tokens=96,
        )
        default_result = runner.call_chat_completion(
            f"http://127.0.0.1:{server.server_port}/v1", "test-secret-do-not-report",
            "glm-5.3-flash", [{"role": "user", "content": "hello"}],
        )
    finally:
        server.shutdown()
        thread.join(timeout=2)
        server.server_close()

    assert observed["authorization"] == "Bearer test-secret-do-not-report"
    assert [body["model"] for body in observed["bodies"]] == ["glm-5.3-flash", "glm-5.3-flash"]
    assert [body["max_tokens"] for body in observed["bodies"]] == [96, 1024]
    assert result["request_id"] == "req-local-1"
    assert result["usage"] == {"prompt_tokens": 17, "completion_tokens": 5, "total_tokens": 22}
    assert result["response_diagnostics"] == {
        "message_content_type": "str",
        "message_content_chars": len('{"allowed":true}'),
        "reasoning_content_present": True,
        "reasoning_content_chars": len("synthetic private reasoning must not be retained"),
        "tool_calls_present": False,
        "refusal_present": False,
    }
    assert default_result["usage"] == result["usage"]
    assert "synthetic private reasoning" not in json.dumps(result)
    assert "test-secret-do-not-report" not in json.dumps(result)


def test_gateway_auth_error_is_safe_and_does_not_retain_provider_body():
    calls = {"count": 0}

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            calls["count"] += 1
            self.rfile.read(int(self.headers["Content-Length"]))
            payload = b'{"error":"invalid API key sk-private-secret-not-for-report"}'
            self.send_response(401)
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        def log_message(self, *_args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        with pytest.raises(runner.ModelEvalError) as captured:
            runner.call_chat_completion(f"http://127.0.0.1:{server.server_port}/v1", "local-test-key",
                                        "test-model", [{"role": "user", "content": "hello"}])
    finally:
        server.shutdown()
        thread.join(timeout=2)
        server.server_close()

    assert captured.value.code == "gateway_auth_error"
    assert "sk-private-secret-not-for-report" not in str(captured.value)
    assert captured.value.safe_detail is None
    assert calls["count"] == 1


def test_gateway_http_400_keeps_only_bounded_redacted_json_diagnostics():
    api_key = "local-test-key-that-must-not-be-saved"
    error_message = f"Model glm-5.1 route unavailable; Bearer {api_key}; " + ("x" * 500)
    payload = json.dumps({"error": {
        "message": error_message,
        "type": "invalid_request_error",
        "code": "model_not_found",
    }}).encode()

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            self.rfile.read(int(self.headers["Content-Length"]))
            self.send_response(400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)

        def log_message(self, *_args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        with pytest.raises(runner.ModelEvalError) as captured:
            runner.call_chat_completion(f"http://127.0.0.1:{server.server_port}/v1", api_key,
                                        "glm-5.1", [{"role": "user", "content": "hello"}])
    finally:
        server.shutdown()
        thread.join(timeout=2)
        server.server_close()

    assert captured.value.code == "gateway_http_400"
    detail = captured.value.safe_detail
    assert "invalid_request_error" in detail
    assert "model_not_found" in detail
    assert "glm-5.1 route unavailable" in detail
    assert "Bearer [REDACTED]" in detail
    assert api_key not in detail
    assert len(detail) <= 300


def test_endpoint_and_budget_guards_fail_closed():
    assert runner.normalize_endpoint("https://ai.xem8k5.top/v1") == "https://ai.xem8k5.top/v1/chat/completions"
    assert runner.normalize_endpoint("http://127.0.0.1:1234/v1") == "http://127.0.0.1:1234/v1/chat/completions"
    with pytest.raises(ValueError):
        runner.normalize_endpoint("http://ai.xem8k5.top/v1")
    with pytest.raises(ValueError):
        runner.validate_run_limits(models=[f"m-{index}" for index in range(21)], case_count=3, max_requests=63,
                                   max_output_tokens=96, budget_yuan=5)
    with pytest.raises(ValueError):
        runner.validate_run_limits(models=["a"], case_count=3, max_requests=3,
                                   max_output_tokens=8193, budget_yuan=5)
    runner.validate_run_limits(models=["a"], case_count=1, max_requests=1,
                               max_output_tokens=1024, budget_yuan=5)
    runner.validate_run_limits(models=["a"], case_count=1, max_requests=1,
                               max_output_tokens=8192, budget_yuan=5)
    with pytest.raises(ValueError):
        runner.validate_run_limits(models=["a"], case_count=1, max_requests=1,
                                   max_output_tokens=8193, budget_yuan=5)
    with pytest.raises(ValueError):
        runner.validate_run_limits(models=["a"], case_count=3, max_requests=3,
                                   max_output_tokens=96, budget_yuan=500.01)
    with pytest.raises(ValueError, match="timeout"):
        runner.validate_run_limits(models=["a"], case_count=1, max_requests=1,
                                   max_output_tokens=96, budget_yuan=5, timeout_seconds=301)


def test_runner_passes_configured_timeout_to_gateway_and_records_it():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "route-scan", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [], "provenance": {"approved": False}}],
    }
    observed_timeouts = []

    def fake_caller(_base_url, _api_key, model, _messages, **kwargs):
        observed_timeouts.append(kwargs["timeout_seconds"])
        return {"content": "hello", "usage": None, "returned_model": model,
                "request_id": "req-timeout-config", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["deepseek-v4-pro"], base_url="https://gateway.example/v1",
        api_key="local-test-key", caller=fake_caller, max_requests=1,
        case_ids=["model-001"], timeout_seconds=120,
    )

    assert observed_timeouts == [120]
    assert report["generation_configuration"]["timeout_seconds"] == 120


def test_runner_attributes_length_finish_with_incomplete_json_to_output_token_limit():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "route-scan", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "return json"}]},
                   "deterministic_checks": [{"check_id": "schema", "kind": "json_schema",
                                             "expected": {"required": {"allowed": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }

    def fake_caller(_base_url, _api_key, model, _messages, **_kwargs):
        return {"content": "", "usage": {"completion_tokens": 96}, "returned_model": model,
                "request_id": "req-truncated-json", "finish_reason": "length", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["hy3"], base_url="https://gateway.example/v1", api_key="local-test-key",
        caller=fake_caller, max_requests=1, max_output_tokens=96, case_ids=["model-001"],
    )

    result = report["results"][0]
    assert result["call_status"] == "success"
    assert result["finish_reason"] == "length"
    assert result["deterministic_status"] == "not_evaluated"
    assert result["evaluation_status"] == "incomplete_output"
    assert result["failure_reason"] == "output_truncated_at_token_limit"
    assert result["raw_output"] == ""
    assert report["deterministic_evaluated_count"] == 0
    assert report["deterministic_pass_count"] == 0
    assert report["deterministic_fail_count"] == 0
    assert report["incomplete_output_count"] == 1
    assert report["quality_gate_status"] == "failed"
    case_summary = report["per_case_model_repetition_summary"][0]
    assert case_summary["deterministic_evaluated_count"] == 0
    assert case_summary["incomplete_output_count"] == 1
    assert case_summary["pass_consistency"] == "no_evaluable_attempts"
    assert case_summary["exact_output_consistency"] is None
    model_summary = report["per_model_summary"][0]
    assert model_summary["incomplete_output_count"] == 1
    assert model_summary["deterministic_evaluated_count"] == 0
    assert model_summary["attempt_pass_rate"] is None


def test_gateway_credential_never_falls_back_to_official_openai_environment_variable():
    assert runner.configured_api_key({"OPENAI_API_KEY": "official-or-unknown-key"}) == ""
    assert runner.configured_api_key({"PROMPTOPS_PROVIDER_API_KEY": "dedicated-gateway-key",
                                      "OPENAI_API_KEY": "official-or-unknown-key"}) == "dedicated-gateway-key"


def test_model_groups_select_dedicated_group_keys_before_universal_gateway_key():
    environment = {
        "PROMPTOPS_PROVIDER_API_KEY": "universal-key",
        "PROMPTOPS_PROVIDER_API_KEY_GUOMODEL": "guomodel-key",
        "PROMPTOPS_PROVIDER_API_KEY_GEMINI": "gemini-key",
        "OPENAI_API_KEY": "official-or-unknown-key",
    }

    assert runner.model_group_for("glm-5.3-flash") == "guomodel"
    assert runner.model_group_for("deepseek-v4.1-flash") == "guomodel"
    assert runner.model_group_for("gemini-3.8-flash-low") == "gemini"
    assert runner.configured_api_key(environment, model="glm-5.3-flash") == "guomodel-key"
    assert runner.configured_api_key(environment, model="gemini-3.8-flash-low") == "gemini-key"
    assert runner.configured_api_key(environment, model="other-model") == "universal-key"


def test_all_models_reported_by_the_users_guomodel_catalog_route_to_the_group_key():
    models = [
        "deepseek-v4-pro", "deepseek-v4.1-flash", "glm-5.1", "glm-5.2", "glm-5.3",
        "glm-5.3-flash", "hy3", "hy4-preview", "kimi-k2.8-preview", "kimi-k3-1",
        "kimi-k3.1-pro", "mimo-v2.6-pro", "minimax-m3", "minimax-m3.1-flash", "step-5-preview",
    ]
    environment = {"PROMPTOPS_PROVIDER_API_KEY_GUOMODEL": "group-local-secret"}

    assert all(runner.model_group_for(model) == "guomodel" for model in models)
    assert all(runner.configured_api_key(environment, model=model) == "group-local-secret" for model in models)


def test_runner_routes_each_model_to_its_group_key_without_recording_credentials():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1", "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "test", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "json", "kind": "json_schema",
                                             "expected": {"required": {"ok": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }
    selected = []

    def fake_caller(_base_url, api_key, model, _messages, **_kwargs):
        selected.append((model, api_key))
        return {"content": "{\"ok\":true}", "usage": None, "returned_model": model,
                "request_id": "req-1", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["glm-5.3-flash", "gemini-3.8-flash-low"], base_url="https://gateway.example/v1",
        api_key="universal-key", api_keys_by_group={"guomodel": "guomodel-secret", "gemini": "gemini-secret"},
        caller=fake_caller, max_requests=2,
    )

    assert selected == [("glm-5.3-flash", "guomodel-secret"), ("gemini-3.8-flash-low", "gemini-secret")]
    assert [result["model_group"] for result in report["results"]] == ["guomodel", "gemini"]
    assert "guomodel-secret" not in json.dumps(report)
    assert "gemini-secret" not in json.dumps(report)


def test_runner_persists_only_redacted_gateway_error_detail():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "test", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [], "provenance": {"approved": False}}],
    }
    api_key = "private-model-group-key"

    def fail_with_gateway_diagnostic(*_args, **_kwargs):
        raise runner.ModelEvalError(
            "gateway_http_400", "Gateway returned HTTP 400; raw response body was not retained.",
            safe_detail=f"code=model_not_found; message=route rejected Bearer {api_key}",
        )

    report = runner.run_exploratory(
        dataset, ["glm-5.1"], base_url="https://gateway.example/v1", api_key=api_key,
        caller=fail_with_gateway_diagnostic, max_requests=1,
    )
    result = report["results"][0]
    serialized = json.dumps(report)

    assert result["call_status"] == "error"
    assert result["error_code"] == "gateway_http_400"
    assert result["error_detail"] == "code=model_not_found; message=route rejected Bearer [REDACTED]"
    assert api_key not in serialized


def test_route_scan_continues_after_model_not_found_and_records_remaining_models():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "route-scan", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "schema", "kind": "json_schema",
                                             "expected": {"required": {"allowed": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }
    attempted_models = []

    def fake_caller(_base_url, _api_key, model, _messages, **_kwargs):
        attempted_models.append(model)
        if model == "glm-5.1":
            raise runner.ModelEvalError(
                "gateway_http_400", "Gateway returned HTTP 400; raw response body was not retained.",
                safe_detail="type=invalid_request_error; code=model_not_found; message=unknown provider for model glm-5.1",
            )
        return {"content": '{"allowed":true}', "usage": None, "returned_model": model,
                "request_id": "req-route-2", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["glm-5.1", "deepseek-v4-pro"], base_url="https://gateway.example/v1",
        api_key="local-test-key", caller=fake_caller, max_requests=2,
        case_ids=["model-001"], continue_on_model_not_found=True,
    )

    assert attempted_models == ["glm-5.1", "deepseek-v4-pro"]
    assert report["actual_calls"] == report["requested_calls"] == 2
    assert report["gateway_error_count"] == 1
    assert report["execution_status"] == "completed_with_errors"
    assert report["quality_gate_status"] == "failed"
    assert [result["call_status"] for result in report["results"]] == ["error", "success"]
    assert report["results"][0]["error_detail"].startswith("type=invalid_request_error; code=model_not_found")


def test_route_scan_can_continue_after_timeout_without_retrying_timed_out_model():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "route-scan", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "schema", "kind": "json_schema",
                                             "expected": {"required": {"allowed": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }
    attempted_models = []

    def fake_caller(_base_url, _api_key, model, _messages, **_kwargs):
        attempted_models.append(model)
        if model == "hy3":
            raise runner.ModelEvalError("gateway_timeout", "Gateway request timed out.")
        return {"content": '{"allowed":true}', "usage": None, "returned_model": model,
                "request_id": "req-route-after-timeout", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["hy3", "glm-5.2"], base_url="https://gateway.example/v1",
        api_key="local-test-key", caller=fake_caller, max_requests=2,
        case_ids=["model-001"], continue_on_gateway_timeout=True,
    )

    assert attempted_models == ["hy3", "glm-5.2"]
    assert report["actual_calls"] == report["requested_calls"] == 2
    assert report["gateway_error_count"] == 1
    assert report["execution_status"] == "completed_with_errors"
    assert report["quality_gate_status"] == "failed"
    assert [result["call_status"] for result in report["results"]] == ["error", "success"]
    assert report["results"][0]["error_code"] == "gateway_timeout"
    assert report["results"][0]["billing_status"] == "not_reported"


def test_multi_case_timeout_continuation_moves_on_without_retrying_timed_out_case():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [
            {"case_id": f"model-00{index}", "scenario_family": f"case-{index}", "split": "development",
             "risk_level": "low", "input": {"turns": [{"role": "user", "content": f"case {index}"}]},
             "deterministic_checks": [{"check_id": "schema", "kind": "json_schema",
                                       "expected": {"required": {"allowed": "boolean"}}}],
             "provenance": {"approved": False}}
            for index in (1, 2)
        ],
    }
    attempted_messages = []

    def fake_caller(_base_url, _api_key, model, messages, **_kwargs):
        attempted_messages.append(messages[0]["content"])
        if messages[0]["content"] == "case 1":
            raise runner.ModelEvalError("gateway_timeout", "Gateway request timed out.")
        return {"content": '{"allowed":true}', "usage": None, "returned_model": model,
                "request_id": "req-after-timeout", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["hy4-preview"], base_url="https://gateway.example/v1", api_key="local-test-key",
        caller=fake_caller, max_requests=2, case_ids=["model-001", "model-002"],
        continue_on_gateway_timeout=True,
    )

    assert attempted_messages == ["case 1", "case 2"]
    assert report["actual_calls"] == report["requested_calls"] == 2
    assert report["gateway_error_count"] == 1
    assert report["execution_status"] == "completed_with_errors"
    assert report["quality_gate_status"] == "failed"
    assert [item["case_id"] for item in report["results"]] == ["model-001", "model-002"]
    assert [item["call_status"] for item in report["results"]] == ["error", "success"]
    assert report["results"][0]["error_code"] == "gateway_timeout"
    assert report["results"][1]["deterministic_status"] == "passed"


def test_network_error_continuation_moves_to_next_case_without_retrying_failed_case():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [
            {"case_id": f"model-00{index}", "scenario_family": f"case-{index}", "split": "development",
             "risk_level": "low", "input": {"turns": [{"role": "user", "content": f"case {index}"}]},
             "deterministic_checks": [{"check_id": "schema", "kind": "json_schema",
                                       "expected": {"required": {"allowed": "boolean"}}}],
             "provenance": {"approved": False}}
            for index in (1, 2)
        ],
    }
    attempted_messages = []

    def fake_caller(_base_url, _api_key, model, messages, **_kwargs):
        attempted_messages.append(messages[0]["content"])
        if messages[0]["content"] == "case 1":
            raise runner.ModelEvalError("gateway_network_error", "Gateway request failed; connection details were not retained.")
        return {"content": '{"allowed":true}', "usage": None, "returned_model": model,
                "request_id": "req-after-network-error", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["deepseek-v4-pro"], base_url="https://gateway.example/v1", api_key="local-test-key",
        caller=fake_caller, max_requests=2, case_ids=["model-001", "model-002"],
        continue_on_gateway_network_error=True,
    )

    assert attempted_messages == ["case 1", "case 2"]
    assert report["actual_calls"] == report["requested_calls"] == 2
    assert report["gateway_error_count"] == 1
    assert report["execution_status"] == "completed_with_errors"
    assert report["quality_gate_status"] == "failed"
    assert [item["call_status"] for item in report["results"]] == ["error", "success"]
    assert report["results"][0]["error_code"] == "gateway_network_error"
    assert report["results"][1]["deterministic_status"] == "passed"


def test_timeout_continue_mode_rejects_repeated_attempts_before_calls():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "route-scan", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [], "provenance": {"approved": False}}],
    }
    attempted = []

    def fake_caller(*_args, **_kwargs):
        attempted.append(True)
        raise AssertionError("route scan must reject repeated attempts before sending requests")

    with pytest.raises(ValueError, match="requires exactly one repetition"):
        runner.run_exploratory(
            dataset, ["hy3"], base_url="https://gateway.example/v1", api_key="local-test-key",
            caller=fake_caller, max_requests=2, case_ids=["model-001"], repetitions=2,
            continue_on_gateway_timeout=True,
        )

    assert attempted == []


def test_route_scan_continue_mode_rejects_multiple_cases_before_calls():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [
            {"case_id": f"model-00{index}", "scenario_family": f"route-{index}", "split": "development",
             "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
             "deterministic_checks": [], "provenance": {"approved": False}}
            for index in (1, 2)
        ],
    }
    attempted = []

    def fake_caller(*_args, **_kwargs):
        attempted.append(True)
        raise AssertionError("route scan must reject multiple cases before sending requests")

    with pytest.raises(ValueError, match="requires exactly one selected case"):
        runner.run_exploratory(
            dataset, ["glm-5.1"], base_url="https://gateway.example/v1", api_key="local-test-key",
            caller=fake_caller, max_requests=2, case_ids=["model-001", "model-002"],
            continue_on_model_not_found=True,
        )

    assert attempted == []


def test_missing_model_group_key_fails_before_any_gateway_call():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1", "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "test", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "json", "kind": "json_schema",
                                             "expected": {"required": {"ok": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }
    calls = []

    with pytest.raises(ValueError, match="gemini"):
        runner.run_exploratory(
            dataset, ["glm-5.3-flash", "gemini-3.8-flash-low"], base_url="https://gateway.example/v1",
            api_keys_by_group={"guomodel": "guomodel-secret"}, caller=lambda *args, **kwargs: calls.append(args),
            max_requests=2,
        )

    assert calls == []


def test_repository_pilot_dataset_is_distinct_draft_data_and_requires_explicit_opt_in():
    path = Path(__file__).with_name("dataset.v0.1.0.json")

    with pytest.raises(ValueError, match="unapproved draft"):
        runner.load_dataset(path, allow_draft=False)

    dataset = runner.load_dataset(path, allow_draft=True)

    assert dataset["status"] == "exploratory_draft_pilot"
    assert len(dataset["cases"]) == 3
    assert len({case["case_id"] for case in dataset["cases"]}) == 3
    assert all(case["provenance"]["approved"] is False for case in dataset["cases"])
    assert all(case["split"] == "development" for case in dataset["cases"])


def test_v0_1_1_reason_code_assertions_are_defined_in_each_model_prompt():
    path = Path(__file__).with_name("dataset.v0.1.1.json")
    dataset = runner.load_dataset(path, allow_draft=True)
    expected_reason_codes = {
        "model-001": "within_limit",
        "model-002": "exceeds_remaining_amount",
        "model-003": "within_limit",
    }

    assert dataset["dataset_version"] == "0.1.1"
    for case in dataset["cases"]:
        prompt = "\n".join(turn["content"] for turn in case["input"]["turns"])
        expected = expected_reason_codes[case["case_id"]]
        assert "reason_code" in prompt
        assert "within_limit" in prompt
        assert "exceeds_remaining_amount" in prompt
        reason_checks = [check for check in case["deterministic_checks"] if check["check_id"] == "reason"]
        assert len(reason_checks) == 1
        assert reason_checks[0]["expected"] == expected


def test_cli_defaults_to_the_latest_corrected_model_dataset(tmp_path, monkeypatch, capsys):
    for name in ("PROMPTOPS_PROVIDER_API_KEY", "PROMPTOPS_PROVIDER_API_KEY_GUOMODEL",
                 "PROMPTOPS_PROVIDER_API_KEY_GEMINI"):
        monkeypatch.delenv(name, raising=False)
    monkeypatch.setenv("OPENAI_API_KEY", "must-not-be-used")
    observed = {}

    def load_dataset(path, *, allow_draft):
        observed["dataset"] = Path(path).name
        assert allow_draft is True
        return {"cases": [{"case_id": "test"}]}

    monkeypatch.setattr(runner, "load_dataset", load_dataset)

    exit_code = runner.main(["--allow-draft", "--execute", "--output-dir", str(tmp_path)])
    captured = capsys.readouterr()

    assert exit_code == 2
    assert observed["dataset"] == "dataset.v0.2.0.json"
    assert "No dedicated gateway credential" in captured.err


def test_report_is_explicitly_non_benchmark_and_keeps_key_out_of_evidence():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1", "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "test", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "json", "kind": "json_schema",
                                             "expected": {"required": {"ok": "boolean"}}}],
                   "provenance": {"approved": False}, "private_note": "private-key"}],
    }

    report = runner.run_exploratory(
        dataset, ["test-model"], base_url="https://gateway.example/v1", api_key="private-key",
        caller=lambda *_args, **_kwargs: {"content": "{\"ok\":true} private-key", "usage": None,
                                          "returned_model": "test-model", "request_id": "req-1",
                                          "finish_reason": "stop", "latency_ms": 1.0},
        max_requests=1,
    )

    serialized = json.dumps(report)
    assert report["benchmark_eligible"] is False
    assert report["human_review_status"] == "pending"
    assert report["actual_calls"] == 1
    assert report["declared_budget_yuan"] == runner.MAX_BUDGET_YUAN
    assert "authorized_budget_yuan" not in report
    assert "private-key" not in serialized
    assert report["results"][0]["provider_reported_usage"] is None
    assert report["results"][0]["case_snapshot"]["private_note"] == "[REDACTED]"


def test_json_schema_checks_enum_values():
    case = case_with_checks([{
        "check_id": "schema",
        "kind": "json_schema",
        "expected": {
            "required": {"reason_code": "string"},
            "properties": {"reason_code": {"enum": ["within_limit", "invalid_amount"]}},
        },
    }])

    passed = runner.evaluate_response(case, '{"reason_code":"within_limit"}')
    failed = runner.evaluate_response(case, '{"reason_code":"probably_ok"}')

    assert passed[0]["passed"] is True
    assert failed[0]["passed"] is False


def test_json_schema_rejects_nonstandard_nan_constant():
    case = case_with_checks([{
        "check_id": "schema",
        "kind": "json_schema",
        "expected": {"required": {"amount": ["number", "null"]}},
    }])

    result = runner.evaluate_response(case, '{"amount":NaN}')

    assert result[0]["passed"] is False
    assert result[0]["actual"] == "Output is not valid JSON."


def test_json_schema_rejects_duplicate_object_keys():
    case = case_with_checks([{
        "check_id": "schema",
        "kind": "json_schema",
        "expected": {"required": {"allowed": "boolean"}},
    }])

    result = runner.evaluate_response(case, '{"allowed":false,"allowed":true}')

    assert result[0]["passed"] is False
    assert result[0]["actual"] == "Output is not valid JSON."


def test_v0_2_dataset_has_unique_reviewable_cases_and_consistent_golden_checks():
    path = Path(__file__).with_name("dataset.v0.2.0.json")
    dataset = runner.load_dataset(path, allow_draft=True)
    cases = dataset["cases"]

    assert dataset["dataset_version"] == "0.2.0"
    assert len(cases) == 40
    assert len({case["case_id"] for case in cases}) == 40
    assert len({case["scenario_family"] for case in cases}) == 40
    assert len({tuple(turn["content"] for turn in case["input"]["turns"]) for case in cases}) == 40
    assert {split: sum(case["split"] == split for case in cases)
            for split in ("development", "validation", "sealed_test")} == {
                "development": 24, "validation": 8, "sealed_test": 8,
            }
    assert all(case["provenance"]["approved"] is False for case in cases)
    for case in cases:
        assert case["expected_output"]
        checks = {check["path"]: check["expected"] for check in case["deterministic_checks"]
                  if check["kind"] == "business_rule"}
        for key, value in case["expected_output"].items():
            assert checks[f"$.{key}"] == value
    dialogue = next(case for case in cases if case["scenario_family"] == "multi_turn_latest_amount_correction")["input"]["turns"]
    assert [message["role"] for message in dialogue] == ["user", "user", "assistant", "user"]
    assert "70.00" in dialogue[1]["content"] and "15.00" in dialogue[-1]["content"]


def test_v0_2_dataset_generator_reproduces_checked_in_dataset_exactly():
    path = Path(__file__).with_name("dataset.v0.2.0.json")

    assert json.loads(path.read_text(encoding="utf-8")) == dataset_generator.build_dataset()


def test_runner_repeats_cases_in_paired_model_order_and_filters_split():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [
            {"case_id": "model-001", "scenario_family": "dev-a", "split": "development",
             "risk_level": "low", "input": {"turns": [{"role": "user", "content": "a"}]},
             "deterministic_checks": [], "provenance": {"approved": False}},
            {"case_id": "model-002", "scenario_family": "holdout", "split": "sealed_test",
             "risk_level": "low", "input": {"turns": [{"role": "user", "content": "b"}]},
             "deterministic_checks": [], "provenance": {"approved": False}},
        ],
    }
    observed = []

    def caller(_base_url, _api_key, model, messages, **_kwargs):
        observed.append((messages[0]["content"], model))
        return {"content": "{}", "usage": None, "returned_model": model,
                "request_id": f"req-{len(observed)}", "finish_reason": "stop", "latency_ms": 1.0}

    report = runner.run_exploratory(
        dataset, ["model-a", "model-b"], base_url="https://gateway.example/v1",
        api_key="local-test-key", caller=caller, max_requests=4,
        split="development", repetitions=2, case_ids=["model-001"],
    )

    assert report["requested_calls"] == 4
    assert report["actual_calls"] == 4
    assert report["unique_case_count"] == 1
    assert report["selected_split"] == "development"
    assert [result["repetition_index"] for result in report["results"]] == [1, 1, 2, 2]
    assert observed == [("a", "model-a"), ("a", "model-b"), ("a", "model-a"), ("a", "model-b")]
    assert len(report["per_case_model_repetition_summary"]) == 2
    assert all(item["attempt_count"] == 2 and item["exact_output_consistency"] is True
               for item in report["per_case_model_repetition_summary"])
    assert report["selected_case_ids"] == ["model-001"]

    with pytest.raises(ValueError, match="Unknown case ID"):
        runner.run_exploratory(dataset, ["model-a"], base_url="https://gateway.example/v1",
                               api_key="local-test-key", caller=caller, max_requests=1,
                               split="development", case_ids=["not-a-case"])
    with pytest.raises(ValueError, match="must belong to split"):
        runner.run_exploratory(dataset, ["model-a"], base_url="https://gateway.example/v1",
                               api_key="local-test-key", caller=caller, max_requests=1,
                               split="development", case_ids=["model-002"])
    with pytest.raises(ValueError, match="must be unique"):
        runner.run_exploratory(dataset, ["model-a"], base_url="https://gateway.example/v1",
                               api_key="local-test-key", caller=caller, max_requests=2,
                               split="development", case_ids=["model-001", "model-001"])


def test_run_limits_allow_the_planned_144_call_batch_but_reject_unbounded_runs():
    runner.validate_run_limits(models=["glm-5.3-flash", "deepseek-v4.1-flash"], case_count=24,
                               max_requests=144, max_output_tokens=192, budget_yuan=100, repetitions=3)

    with pytest.raises(ValueError, match="cannot exceed"):
        runner.validate_run_limits(models=["model-a"], case_count=1, max_requests=5001,
                                   max_output_tokens=192, budget_yuan=100)


def test_run_limits_allow_catalog_route_preflight_and_full_1080_call_matrix():
    models = dataset_generator.build_dataset()["execution_plan"]["models"]
    runner.validate_run_limits(models=models, case_count=3, max_requests=45,
                               max_output_tokens=192, budget_yuan=500, repetitions=1)
    runner.validate_run_limits(models=models, case_count=24, max_requests=1080,
                               max_output_tokens=192, budget_yuan=500, repetitions=3)


def test_sealed_split_requires_explicit_opt_in():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1",
        "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "sealed", "split": "sealed_test",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [], "provenance": {"approved": False}}],
    }

    with pytest.raises(ValueError, match="sealed_test"):
        runner.run_exploratory(
            dataset, ["model-a"], base_url="https://gateway.example/v1", api_key="local-test-key",
            caller=lambda *_args, **_kwargs: {}, max_requests=1, split="sealed_test",
        )


def test_report_captures_prompt_hash_and_per_model_aggregate_without_cost_fabrication():
    dataset = {
        "dataset_id": "test-dataset", "dataset_version": "0.0.1", "status": "exploratory_draft_pilot",
        "cases": [{"case_id": "model-001", "scenario_family": "test", "split": "development",
                   "risk_level": "low", "input": {"turns": [{"role": "user", "content": "hello"}]},
                   "deterministic_checks": [{"check_id": "json", "kind": "json_schema",
                                             "expected": {"required": {"ok": "boolean"}}}],
                   "provenance": {"approved": False}}],
    }
    report = runner.run_exploratory(
        dataset, ["glm-5.3-flash"], base_url="https://gateway.example/v1", api_key="local-key",
        caller=lambda *_args, **_kwargs: {"content": '{"ok":true}', "usage": {"total_tokens": 7},
                                         "returned_model": "glm-5.3-flash", "request_id": "req-1",
                                         "finish_reason": "stop", "latency_ms": 12.0},
        max_requests=1,
    )

    result = report["results"][0]
    model_summary = report["per_model_summary"][0]
    assert len(result["prompt_sha256"]) == 64
    assert report["execution_status"] == "completed"
    assert report["quality_gate_status"] == "passed"
    assert model_summary["requested_model"] == "glm-5.3-flash"
    assert model_summary["successful_attempt_count"] == 1
    assert model_summary["deterministic_pass_count"] == 1
    assert model_summary["provider_reported_token_totals"]["total_tokens"] == 7
    assert model_summary["provider_reported_cost"] is None


def test_cli_persists_assertion_failures_and_returns_nonzero_quality_gate(tmp_path, monkeypatch, capsys):
    monkeypatch.setenv("PROMPTOPS_PROVIDER_API_KEY_GUOMODEL", "test-local-model-key")
    monkeypatch.delenv("PROMPTOPS_PROVIDER_API_KEY", raising=False)

    def failing_completion(_base_url, _api_key, model, _messages, **_kwargs):
        return {"content": "{}", "usage": None, "returned_model": model, "request_id": "req-fail",
                "finish_reason": "stop", "latency_ms": 1.0}

    monkeypatch.setattr(runner, "call_chat_completion", failing_completion)
    exit_code = runner.main([
        "--allow-draft", "--execute", "--models", "glm-5.3-flash", "--split", "development",
        "--case-ids", "model-001", "--max-requests", "1", "--budget-yuan", "5",
        "--output-dir", str(tmp_path),
    ])

    output = capsys.readouterr().out.splitlines()
    report_path = Path(next(line.removeprefix("report_path=") for line in output
                            if line.startswith("report_path=")))
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert exit_code == 1
    assert report["actual_calls"] == report["requested_calls"] == 1
    assert report["execution_status"] == "completed"
    assert report["gateway_error_count"] == 0
    assert report["deterministic_fail_count"] == 1
    assert report["quality_gate_status"] == "failed"
