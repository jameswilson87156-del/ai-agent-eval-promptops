"""Create a provenance-aware summary from the raw k6 JSON summary."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
from typing import Any


def _metric_values(metrics: dict[str, Any], name: str) -> dict[str, Any]:
    metric = metrics.get(name)
    if not isinstance(metric, dict) or not isinstance(metric.get("values"), dict):
        return {}
    return metric["values"]


def _number(value: Any, default: float = 0.0) -> float:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return default
    return number if number == number else default


def _threshold_failures(metrics: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    for metric_name, metric in metrics.items():
        thresholds = metric.get("thresholds", {}) if isinstance(metric, dict) else {}
        for expression, result in thresholds.items():
            if not isinstance(result, dict) or result.get("ok") is not True:
                failures.append(f"{metric_name}: {expression}")
    return sorted(failures)


def build_report(data: dict[str, Any], source_file: str) -> dict[str, Any]:
    metrics = data.get("metrics")
    metadata = data.get("project_metadata")
    if not isinstance(metrics, dict) or not isinstance(metadata, dict):
        raise ValueError("Input is not a k6 report with PromptOps project metadata.")
    plans = metadata.get("scenario_plans")
    if not isinstance(plans, dict) or not plans:
        raise ValueError("The report does not contain the declared scenario plan.")

    scenarios: list[dict[str, Any]] = []
    missing_metrics: list[str] = []
    required_metric_names = (
        "checks",
        "http_req_failed",
        "business_workflow_success",
        "business_workflow_failures",
        "dropped_iterations",
        "iterations",
    )
    for name, plan in plans.items():
        tagged = {key: f"{key}{{scenario:{name}}}" for key in required_metric_names}
        absent = [metric_name for metric_name in tagged.values() if metric_name not in metrics]
        missing_metrics.extend(absent)
        values = {key: _metric_values(metrics, metric_name) for key, metric_name in tagged.items()}
        scenario_threshold_failures = [
            f"{metric_name}: {expression}"
            for metric_name in tagged.values()
            for expression, result in (
                (metrics.get(metric_name, {}).get("thresholds", {}) or {}).items()
                if isinstance(metrics.get(metric_name), dict)
                else []
            )
            if not isinstance(result, dict) or result.get("ok") is not True
        ]
        for metric_name in tagged.values():
            metric = metrics.get(metric_name)
            thresholds = metric.get("thresholds", {}) if isinstance(metric, dict) else {}
            if not thresholds:
                scenario_threshold_failures.append(f"{metric_name}: missing threshold result")

        measured_iterations = int(_number(values["iterations"].get("count")))
        minimum_iterations = int(_number(plan.get("minimum_iterations")))
        checks_passed = int(_number(values["checks"].get("passes")))
        checks_failed = int(_number(values["checks"].get("fails")))
        scenario_ok = (
            not absent
            and not scenario_threshold_failures
            and measured_iterations >= minimum_iterations
            and checks_failed == 0
            and _number(values["http_req_failed"].get("rate")) == 0
            and _number(values["business_workflow_success"].get("rate")) == 1
            and _number(values["business_workflow_failures"].get("count")) == 0
            and _number(values["dropped_iterations"].get("count")) == 0
        )
        scenarios.append(
            {
                "name": name,
                "target_workflows_per_second": _number(plan.get("target_workflows_per_second")),
                "duration_seconds": int(_number(plan.get("duration_seconds"))),
                "minimum_iterations": minimum_iterations,
                "measured_iterations": measured_iterations,
                "checks_passed": checks_passed,
                "checks_failed": checks_failed,
                "http_failure_rate": _number(values["http_req_failed"].get("rate")),
                "workflow_success_rate": _number(values["business_workflow_success"].get("rate")),
                "business_failures": int(_number(values["business_workflow_failures"].get("count"))),
                "dropped_iterations": int(_number(values["dropped_iterations"].get("count"))),
                "threshold_status": "passed" if scenario_ok else "failed",
                "threshold_failures": sorted(scenario_threshold_failures),
            }
        )

    all_threshold_failures = _threshold_failures(metrics)
    http_duration = _metric_values(metrics, "http_req_duration")
    workflow_duration = _metric_values(metrics, "group_duration")
    iterations = _metric_values(metrics, "iterations")
    requests = _metric_values(metrics, "http_reqs")
    checks = _metric_values(metrics, "checks")
    summary_failed = bool(
        missing_metrics
        or all_threshold_failures
        or any(scenario["threshold_status"] != "passed" for scenario in scenarios)
    )
    return {
        "schema_version": "1.0",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "source_file": Path(source_file).name,
        "run_id": metadata.get("run_id"),
        "execution_mode": metadata.get("run_mode"),
        "target": metadata.get("target"),
        "measurement_boundary": "Local synthetic service only; not production traffic or capacity evidence.",
        "summary": {
            "quality_gate": "failed" if summary_failed else "passed",
            "workflow_count": int(_number(iterations.get("count"))),
            "http_request_count": int(_number(requests.get("count"))),
            "checks_passed": int(_number(checks.get("passes"))),
            "checks_failed": int(_number(checks.get("fails"))),
            "http_failure_rate": _number(_metric_values(metrics, "http_req_failed").get("rate")),
            "workflow_success_rate": _number(_metric_values(metrics, "business_workflow_success").get("rate")),
            "business_failures": int(_number(_metric_values(metrics, "business_workflow_failures").get("count"))),
            "dropped_iterations": int(_number(_metric_values(metrics, "dropped_iterations").get("count"))),
            "active_vus_peak": int(_number(_metric_values(metrics, "vus").get("max"))),
            "maximum_vu_capacity": int(_number(_metric_values(metrics, "vus_max").get("max"))),
            "combined_http_duration_ms": {
                "p50": _number(http_duration.get("med")),
                "p95": _number(http_duration.get("p(95)")),
                "p99": _number(http_duration.get("p(99)")),
            },
            "combined_workflow_duration_ms": {
                "p95": _number(workflow_duration.get("p(95)")),
                "p99": _number(workflow_duration.get("p(99)")),
            },
            "failed_thresholds": all_threshold_failures,
            "missing_scenario_metrics": sorted(missing_metrics),
        },
        "scenarios": scenarios,
    }


def render_markdown(report: dict[str, Any]) -> str:
    summary = report["summary"]
    lines = [
        "# Local k6 load report",
        "",
        f"- Run ID: `{report.get('run_id')}`",
        f"- Gate: **{summary['quality_gate'].upper()}**",
        f"- Mode: `{report.get('execution_mode')}`",
        f"- Target: {report.get('target')}",
        f"- Source JSON: `{report['source_file']}`",
        "- Boundary: local synthetic service; this is not production traffic, SLO, or production capacity evidence.",
        "",
        "## Measured totals",
        "",
        "| Workflows | HTTP requests | Checks passed / failed | HTTP failure rate | Workflow success | Dropped | Active VU peak | VU capacity |",
        "|---:|---:|---:|---:|---:|---:|---:|---:|",
        (
            f"| {summary['workflow_count']} | {summary['http_request_count']} | "
            f"{summary['checks_passed']} / {summary['checks_failed']} | "
            f"{summary['http_failure_rate']:.4%} | {summary['workflow_success_rate']:.4%} | "
            f"{summary['dropped_iterations']} | {summary['active_vus_peak']} | {summary['maximum_vu_capacity']} |"
        ),
        "",
        "## Scenario gates",
        "",
        "| Scenario | Target workflows/s | Minimum workflows | Measured | Checks passed / failed | HTTP failure | Workflow success | Dropped | Gate |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---|",
    ]
    for scenario in report["scenarios"]:
        lines.append(
            f"| {scenario['name']} | {scenario['target_workflows_per_second']:g} | "
            f"{scenario['minimum_iterations']} | {scenario['measured_iterations']} | "
            f"{scenario['checks_passed']} / {scenario['checks_failed']} | "
            f"{scenario['http_failure_rate']:.4%} | {scenario['workflow_success_rate']:.4%} | "
            f"{scenario['dropped_iterations']} | {scenario['threshold_status'].upper()} |"
        )
    http_duration = summary["combined_http_duration_ms"]
    workflow_duration = summary["combined_workflow_duration_ms"]
    lines.extend(
        [
            "",
            "## Combined latency observations",
            "",
            f"- HTTP request duration: p50 {http_duration['p50']:.3f} ms, p95 {http_duration['p95']:.3f} ms, p99 {http_duration['p99']:.3f} ms.",
            f"- Full workflow duration: p95 {workflow_duration['p95']:.3f} ms, p99 {workflow_duration['p99']:.3f} ms.",
            "- These percentiles combine all four stages and are observations from this local run; no service SLO was defined.",
            "",
        ]
    )
    if summary["failed_thresholds"] or summary["missing_scenario_metrics"]:
        lines.extend(["## Gate failures", ""])
        lines.extend(f"- {failure}" for failure in summary["failed_thresholds"])
        lines.extend(f"- Missing: {name}" for name in summary["missing_scenario_metrics"])
        lines.append("")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="Raw k6 JSON summary")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--markdown-out", type=Path)
    args = parser.parse_args()

    try:
        data = json.loads(args.input.read_text(encoding="utf-8"))
        report = build_report(data, args.input.name)
    except (OSError, json.JSONDecodeError, ValueError) as error:
        parser.error(str(error))
    json_out = args.json_out or args.input.with_name(f"{args.input.stem}-summary.json")
    markdown_out = args.markdown_out or args.input.with_name(f"{args.input.stem}-summary.md")
    if args.input.resolve() in {json_out.resolve(), markdown_out.resolve()}:
        parser.error("Output paths must not overwrite the raw k6 report.")
    json_out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    markdown_out.write_text(render_markdown(report), encoding="utf-8")
    print(f"Summary JSON: {json_out}")
    print(f"Summary Markdown: {markdown_out}")
    print(f"Quality gate: {report['summary']['quality_gate']}")
    return 0 if report["summary"]["quality_gate"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
