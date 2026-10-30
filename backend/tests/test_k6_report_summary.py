from backend.tools.summarize_k6_report import build_report, render_markdown


def metric(values, threshold_ok=True):
    result = {"values": values}
    if threshold_ok is not None:
        result["thresholds"] = {"contract": {"ok": threshold_ok}}
    return result


def raw_report():
    metrics = {
        "checks": metric({"passes": 20, "fails": 0, "rate": 1}, True),
        "http_req_failed": metric({"rate": 0}, True),
        "business_workflow_success": metric({"rate": 1, "passes": 2, "fails": 0}, True),
        "business_workflow_failures": metric({"count": 0}, True),
        "dropped_iterations": metric({"count": 0}, True),
        "iterations": metric({"count": 2, "rate": 1}, True),
        "http_reqs": metric({"count": 10, "rate": 5}),
        "http_req_duration": metric({"med": 4, "p(95)": 8, "p(99)": 9}),
        "group_duration": metric({"p(95)": 20, "p(99)": 25}),
        "vus": metric({"max": 3}),
        "vus_max": metric({"max": 20}),
    }
    for name, values in {
        "checks": {"passes": 20, "fails": 0, "rate": 1},
        "http_req_failed": {"rate": 0},
        "business_workflow_success": {"rate": 1, "passes": 2, "fails": 0},
        "business_workflow_failures": {"count": 0},
        "dropped_iterations": {"count": 0},
        "iterations": {"count": 2, "rate": 1},
    }.items():
        metrics[f"{name}{{scenario:smoke}}"] = metric(values, True)
    return {
        "metrics": metrics,
        "project_metadata": {
            "run_id": "k6-smoke-001",
            "run_mode": "local-synthetic-load",
            "target": "PromptOps isolated synthetic order/refund API",
            "scenario_plans": {
                "smoke": {
                    "target_workflows_per_second": 1,
                    "duration_seconds": 2,
                    "minimum_iterations": 2,
                }
            },
        },
    }


def test_summary_uses_scenario_thresholds_and_distinguishes_vu_peak_from_capacity():
    report = build_report(raw_report(), "raw-k6.json")

    assert report["summary"]["quality_gate"] == "passed"
    assert report["summary"]["workflow_count"] == 2
    assert report["summary"]["active_vus_peak"] == 3
    assert report["summary"]["maximum_vu_capacity"] == 20
    assert report["scenarios"][0]["measured_iterations"] == 2
    assert report["scenarios"][0]["threshold_status"] == "passed"


def test_summary_fails_when_a_scenario_threshold_fails():
    data = raw_report()
    data["metrics"]["iterations{scenario:smoke}"]["thresholds"]["contract"]["ok"] = False

    report = build_report(data, "raw-k6.json")

    assert report["summary"]["quality_gate"] == "failed"
    assert report["scenarios"][0]["threshold_status"] == "failed"


def test_summary_fails_closed_when_a_scenario_metric_is_missing():
    data = raw_report()
    del data["metrics"]["checks{scenario:smoke}"]

    report = build_report(data, "raw-k6.json")

    assert report["summary"]["quality_gate"] == "failed"
    assert "checks{scenario:smoke}" in report["summary"]["missing_scenario_metrics"]


def test_markdown_calls_out_local_boundary_and_combined_percentiles():
    markdown = render_markdown(build_report(raw_report(), "raw-k6.json"))

    assert "not production traffic" in markdown
    assert "combine all four stages" in markdown
    assert "3 | 20" in markdown


def ramping_vu_report():
    data = raw_report()
    data["project_metadata"] = {
        "run_id": "k6-vu-smoke-001",
        "run_mode": "local-synthetic-vu-stress",
        "target": "PromptOps isolated synthetic order/refund API",
        "executor": "ramping-vus",
        "target_active_vus": 50,
        "stage_plan": [
            {"duration": "10s", "target": 10, "label": "ramp to 10 VUs"},
            {"duration": "20s", "target": 50, "label": "hold 50 VUs"},
        ],
        "client_think_time_seconds_between_workflows": 1,
    }
    return data


def test_summary_supports_ramping_vus_and_keeps_stage_plan():
    report = build_report(ramping_vu_report(), "raw-vu-k6.json")
    markdown = render_markdown(report)

    assert report["summary"]["quality_gate"] == "passed"
    assert report["execution_mode"] == "local-synthetic-vu-stress"
    assert report["executor"] == "ramping-vus"
    assert report["target_active_vus"] == 50
    assert report["stage_plan"][1]["target"] == 50
    assert report["summary"]["workflow_count"] == 2
    assert "ramp to 10 VUs" in markdown
    assert "hold 50 VUs" in markdown


def test_ramping_vus_summary_fails_when_any_global_threshold_fails():
    data = ramping_vu_report()
    data["metrics"]["http_req_failed"]["thresholds"]["contract"]["ok"] = False

    report = build_report(data, "raw-vu-k6.json")

    assert report["summary"]["quality_gate"] == "failed"
    assert report["summary"]["failed_thresholds"] == ["http_req_failed: contract"]
