"""Audit a model-evaluation dataset before sending requests to an external model gateway."""

from __future__ import annotations

import argparse
from collections import Counter
import csv
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import sys


HERE = Path(__file__).resolve().parent
DEFAULT_DATASET = HERE / "dataset.v0.2.0.json"
DEFAULT_SCHEMA = HERE / "case.v0.2.0.schema.json"
DEFAULT_REVIEW_CSV = HERE / "human-review-v0.2.0.csv"
EXPECTED_OUTPUT_FIELDS = {
    "allowed",
    "requested_refund_cny",
    "remaining_refundable_before_cny",
    "reason_code",
}
SPLITS = {"development", "validation", "sealed_test"}
CANARY_PATTERN = re.compile(r"SYNTHETIC-CANARY-[A-Z0-9-]+")
TOOL_CHECK_KINDS = {"tool_name", "tool_arguments", "tool_order", "tool_count", "forbidden_tool"}


def _check(name: str, status: str, details: list[str] | None = None, **metrics: object) -> dict:
    result = {"name": name, "status": status, "details": details or []}
    if metrics:
        result["metrics"] = metrics
    return result


def _load_json(path: Path) -> tuple[dict, bytes]:
    raw = path.read_bytes()
    value = json.loads(raw.decode("utf-8-sig"))
    if not isinstance(value, dict):
        raise ValueError("Dataset root must be a JSON object.")
    return value, raw


def _case_schema_check(cases: list, schema_path: Path | None) -> dict:
    if schema_path is None:
        return _check("case_json_schema", "skipped", ["No case schema was supplied."])
    try:
        schema = json.loads(schema_path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as error:
        return _check("case_json_schema", "failed", [f"Could not load schema ({error.__class__.__name__})."])
    try:
        from jsonschema import Draft202012Validator
    except ImportError:
        return _check("case_json_schema", "skipped", ["Optional package jsonschema is not installed."])

    try:
        validator = Draft202012Validator(schema)
    except Exception as error:
        return _check("case_json_schema", "failed", [f"Schema is not a valid Draft 2020-12 schema ({error.__class__.__name__})."])
    errors = []
    for case in cases:
        case_id = case.get("case_id", "<missing>") if isinstance(case, dict) else "<not-object>"
        for error in validator.iter_errors(case):
            location = ".".join(str(part) for part in error.absolute_path) or "$"
            errors.append(f"{case_id} {location}: {error.message}")
            if len(errors) >= 40:
                errors.append("Further schema issues were omitted.")
                break
        if len(errors) >= 40:
            break
    return _check(
        "case_json_schema",
        "failed" if errors else "passed",
        errors,
        case_count=len(cases),
        schema_path=schema_path.name,
    )


def _expected_check(cases: list) -> dict:
    issues = []
    for case in cases:
        if not isinstance(case, dict):
            continue
        case_id = case.get("case_id", "<missing>")
        expected = case.get("expected_output")
        if not isinstance(expected, dict) or set(expected) != EXPECTED_OUTPUT_FIELDS:
            issues.append(f"{case_id}: expected_output must contain exactly the four refund decision fields.")
            continue
        checks = case.get("deterministic_checks")
        if not isinstance(checks, list):
            issues.append(f"{case_id}: deterministic_checks must be a list.")
            continue
        rule_values: dict[str, object] = {}
        duplicate_paths = set()
        for item in checks:
            if not isinstance(item, dict) or item.get("kind") != "business_rule":
                continue
            path = item.get("path", "")
            if not isinstance(path, str) or not path.startswith("$."):
                continue
            field = path[2:]
            if field in rule_values:
                duplicate_paths.add(field)
            rule_values[field] = item.get("expected")
        if duplicate_paths:
            issues.append(f"{case_id}: duplicate business-rule path(s): {', '.join(sorted(duplicate_paths))}.")
        if set(rule_values) != EXPECTED_OUTPUT_FIELDS:
            issues.append(f"{case_id}: business-rule checks must cover exactly the four expected output fields.")
            continue
        mismatches = [field for field in sorted(EXPECTED_OUTPUT_FIELDS) if rule_values[field] != expected[field]]
        if mismatches:
            issues.append(f"{case_id}: golden and deterministic check disagree for {', '.join(mismatches)}.")
    return _check("golden_matches_deterministic_checks", "failed" if issues else "passed", issues)


def _worksheet_check(dataset: dict, dataset_sha256: str, case_ids: set[str], worksheet_path: Path | None) -> tuple[dict, dict]:
    if worksheet_path is None:
        check = _check("review_worksheet_matches_dataset", "skipped", ["No human-review worksheet was supplied."])
        return check, {"row_count": 0, "reviewed_case_count": 0, "pending_case_count": len(case_ids), "approved_case_count": 0}
    try:
        with worksheet_path.open("r", encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle)
            rows = list(reader)
            headers = set(reader.fieldnames or [])
    except (OSError, UnicodeError, csv.Error) as error:
        check = _check("review_worksheet_matches_dataset", "failed", [f"Could not read worksheet ({error.__class__.__name__})."])
        return check, {"row_count": 0, "reviewed_case_count": 0, "pending_case_count": len(case_ids), "approved_case_count": 0}

    required_headers = {
        "dataset_id", "dataset_version", "dataset_sha256", "case_id", "provenance_approved",
        "review_decision", "reviewer", "reviewed_at_utc",
    }
    issues = []
    if not required_headers.issubset(headers):
        issues.append("Worksheet is missing required columns: " + ", ".join(sorted(required_headers - headers)) + ".")
    row_ids = [row.get("case_id", "") for row in rows]
    row_id_set = set(row_ids)
    if len(row_ids) != len(row_id_set):
        issues.append("Worksheet contains duplicate case IDs.")
    if row_id_set != case_ids:
        issues.append(f"Worksheet case IDs differ from dataset (missing={len(case_ids - row_id_set)}, extra={len(row_id_set - case_ids)}).")
    for row in rows:
        if row.get("dataset_id") != dataset.get("dataset_id"):
            issues.append("Worksheet dataset_id does not match the dataset.")
            break
    if any(row.get("dataset_version") != str(dataset.get("dataset_version")) for row in rows):
        issues.append("Worksheet dataset_version does not match the dataset.")
    if any(row.get("dataset_sha256") != dataset_sha256 for row in rows):
        issues.append("Worksheet dataset SHA-256 does not match the current dataset file.")
    cases_by_id = {case.get("case_id"): case for case in dataset.get("cases", []) if isinstance(case, dict)}
    for row in rows:
        case = cases_by_id.get(row.get("case_id"))
        if case and str(row.get("provenance_approved") or "").strip().lower() != str(case.get("provenance", {}).get("approved", False)).lower():
            issues.append(f"{row.get('case_id')}: worksheet approval snapshot differs from dataset provenance.")
            break
    reviewed_rows = [row for row in rows if all(str(row.get(key) or "").strip() for key in ("review_decision", "reviewer", "reviewed_at_utc"))]
    approved_rows = [row for row in reviewed_rows if row.get("review_decision", "").strip().lower() in {"approve", "approved"}]
    review_status = {
        "row_count": len(rows),
        "reviewed_case_count": len(reviewed_rows),
        "pending_case_count": max(len(case_ids) - len(reviewed_rows), 0),
        "approved_case_count": len(approved_rows),
    }
    return _check("review_worksheet_matches_dataset", "failed" if issues else "passed", issues,
                  worksheet_path=worksheet_path.name, row_count=len(rows)), review_status


def audit_dataset(dataset_path: Path, schema_path: Path | None = None,
                  worksheet_path: Path | None = None) -> dict:
    dataset_path = Path(dataset_path)
    schema_path = Path(schema_path) if schema_path else None
    worksheet_path = Path(worksheet_path) if worksheet_path else None
    dataset, raw = _load_json(dataset_path)
    cases = dataset.get("cases")
    if not isinstance(cases, list) or not cases:
        raise ValueError("Dataset must contain at least one case.")
    # Git may check out JSON text with CRLF on Windows and LF on Linux. Hash
    # line-ending-normalized file bytes so the worksheet is portable across OSes.
    digest = hashlib.sha256(raw.replace(b"\r\n", b"\n")).hexdigest()

    case_ids = [case.get("case_id") for case in cases if isinstance(case, dict)]
    family_values = [case.get("scenario_family") for case in cases if isinstance(case, dict)]
    valid_case_ids = [value for value in case_ids if isinstance(value, str)]
    valid_families = [value for value in family_values if isinstance(value, str)]
    split_counts = Counter(str(case.get("split") or "") for case in cases if isinstance(case, dict))
    category_counts = Counter(str(case.get("category") or "uncategorized") for case in cases if isinstance(case, dict))
    declared_splits = (dataset.get("planned_size") or {}).get("actual_split_counts", {})
    actual_splits = {name: split_counts.get(name, 0) for name in sorted(SPLITS)}
    unexpected_splits = sorted(set(split_counts) - SPLITS)

    checks = [
        _case_schema_check(cases, schema_path),
        _check("unique_case_ids", "passed" if all(isinstance(value, str) and value.strip() for value in case_ids)
               and len(valid_case_ids) == len(set(valid_case_ids)) and len(case_ids) == len(cases) else "failed",
               [] if all(isinstance(value, str) and value.strip() for value in case_ids)
               and len(valid_case_ids) == len(set(valid_case_ids)) and len(case_ids) == len(cases) else ["Every case must have a unique non-empty case_id."],
               unique_count=len(set(valid_case_ids)), case_count=len(cases)),
        _check("unique_scenario_families", "passed" if all(isinstance(value, str) and value.strip() for value in family_values)
               and len(valid_families) == len(set(valid_families)) and len(family_values) == len(cases) else "failed",
               [] if all(isinstance(value, str) and value.strip() for value in family_values)
               and len(valid_families) == len(set(valid_families)) and len(family_values) == len(cases) else ["Each case must have a unique non-empty scenario_family."],
               unique_count=len(set(valid_families)), case_count=len(cases)),
        _check("split_counts_match_metadata", "passed" if actual_splits == declared_splits and not unexpected_splits else "failed",
               [] if actual_splits == declared_splits and not unexpected_splits else [f"Declared split counts {declared_splits!r} do not match actual counts {actual_splits!r}; unexpected splits={unexpected_splits!r}."],
               actual=actual_splits, declared=declared_splits, unexpected=unexpected_splits),
        _expected_check(cases),
    ]

    check_issues = []
    turn_issues = []
    injection_issues = []
    approval_issues = []
    synthetic_boundary_issues = []
    tool_use_case_count = 0
    approved_count = 0
    for case in cases:
        if not isinstance(case, dict):
            continue
        case_id = case.get("case_id", "<missing>")
        case_checks = case.get("deterministic_checks", [])
        if not isinstance(case_checks, list):
            check_issues.append(f"{case_id}: deterministic_checks must be a list.")
            case_checks = []
        check_ids = [item.get("check_id") for item in case_checks if isinstance(item, dict)] if isinstance(case_checks, list) else []
        if (not all(isinstance(value, str) and value.strip() for value in check_ids)
                or len(check_ids) != len(set(check_ids))
                or len(check_ids) != len(case_checks if isinstance(case_checks, list) else [])):
            check_issues.append(f"{case_id}: deterministic check IDs must be unique and non-empty.")
        tool_checks = [item for item in case_checks if isinstance(item, dict) and item.get("kind") in TOOL_CHECK_KINDS]
        available_tools = (case.get("input") or {}).get("available_tools", [])
        if available_tools or tool_checks:
            tool_use_case_count += 1
        input_data = case.get("input") or {}
        context = input_data.get("context") if isinstance(input_data, dict) else None
        if not isinstance(context, dict) or context.get("data_kind") != "synthetic" or available_tools != []:
            synthetic_boundary_issues.append(f"{case_id}: v0.2.0 cases must declare synthetic input and no available tools.")
        turns = input_data.get("turns") if isinstance(input_data, dict) else None
        if not isinstance(turns, list) or not turns:
            turn_issues.append(f"{case_id}: input.turns must be a non-empty list.")
            continue
        roles = [turn.get("role") for turn in turns if isinstance(turn, dict)]
        if len(roles) != len(turns) or any(role not in {"user", "assistant"} for role in roles) or roles[-1:] != ["user"]:
            turn_issues.append(f"{case_id}: turns must use user/assistant roles and end with a user request.")
        prompt = "\n".join(turn.get("content", "") for turn in turns if isinstance(turn, dict))
        canaries = set(CANARY_PATTERN.findall(prompt))
        negative_values = [item.get("expected") for item in case_checks
                           if isinstance(item, dict) and item.get("kind") == "not_contains"]
        for canary in sorted(canaries):
            if canary not in negative_values:
                injection_issues.append(f"{case_id}: input Canary {canary} has no matching not_contains assertion.")
        if case.get("category") == "prompt_injection" and canaries and not negative_values:
            injection_issues.append(f"{case_id}: prompt-injection Canary is not guarded by a negative assertion.")
        provenance = case.get("provenance") or {}
        approved = provenance.get("approved")
        if approved is True:
            approved_count += 1
            if not provenance.get("approved_by") or not provenance.get("approved_at"):
                approval_issues.append(f"{case_id}: approved case is missing approver or approval timestamp.")
        elif approved is not False:
            approval_issues.append(f"{case_id}: provenance.approved must be an explicit boolean.")

    checks.extend([
        _check("unique_deterministic_check_ids", "failed" if check_issues else "passed", check_issues),
        _check("input_turn_contract", "failed" if turn_issues else "passed", turn_issues),
        _check("synthetic_data_boundary", "failed" if synthetic_boundary_issues else "passed", synthetic_boundary_issues),
        _check("prompt_injection_canary_guard", "failed" if injection_issues else "passed", injection_issues),
        _check("approval_provenance_contract", "failed" if approval_issues else "passed", approval_issues),
    ])
    worksheet_check, worksheet_status = _worksheet_check(dataset, digest, set(valid_case_ids), worksheet_path)
    checks.append(worksheet_check)

    blockers = dataset.get("benchmark_blockers", [])
    blocker_list = blockers if isinstance(blockers, list) else []
    declared_eligible = dataset.get("benchmark_eligible") is True
    review_complete = worksheet_status["pending_case_count"] == 0 and worksheet_status["approved_case_count"] == len(cases)
    benchmark_eligible = bool(declared_eligible and approved_count == len(cases) and review_complete and not blocker_list)
    if declared_eligible and not benchmark_eligible:
        checks.append(_check("benchmark_eligibility_guard", "failed", ["Dataset claims benchmark eligibility while approval/review/blocker requirements are unmet."]))
    else:
        checks.append(_check("benchmark_eligibility_guard", "passed", []))

    counts = Counter(check["status"] for check in checks)
    errors = counts.get("failed", 0)
    schema_status = next(check["status"] for check in checks if check["name"] == "case_json_schema")
    readiness_blockers = []
    if errors:
        readiness_blockers.append(f"{errors} required audit check(s) failed.")
    if schema_status != "passed":
        readiness_blockers.append("Draft 2020-12 case schema validation did not complete.")
    scope_disclaimers = []
    if tool_use_case_count == 0:
        scope_disclaimers.append("No tool calls are enabled or asserted; this dataset does not evaluate Agent tool use.")
    if dataset.get("benchmark_eligible") is False:
        scope_disclaimers.append("This dataset is explicitly marked benchmark_eligible=false.")
    if any("not a confidential blind holdout" in str(blocker).lower() for blocker in blocker_list):
        scope_disclaimers.append("The sealed_test candidate is visible in the repository and is not blind.")

    return {
        "audit_schema_version": "1.0.0",
        "created_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "dataset": {
            "dataset_id": dataset.get("dataset_id"),
            "dataset_version": dataset.get("dataset_version"),
            "status": dataset.get("status"),
            "case_count": len(cases),
            "sha256": digest,
            "split_counts": actual_splits,
            "category_counts": dict(sorted(category_counts.items())),
            "scenario_family_count": len(set(valid_families)),
        },
        "summary": {
            "passed": counts.get("passed", 0),
            "failed": errors,
            "skipped": counts.get("skipped", 0),
            "warning": counts.get("warning", 0),
        },
        "checks": checks,
        "ready_for_exploratory_run": errors == 0 and schema_status == "passed",
        "readiness_blockers": readiness_blockers,
        "benchmark_eligible": benchmark_eligible,
        "human_review": {
            **worksheet_status,
            "dataset_approved_case_count": approved_count,
            "rubric_version": dataset.get("rubric_version"),
        },
        "scope": {
            "domain_scope": dataset.get("domain_scope"),
            "tool_use_evaluation": tool_use_case_count > 0,
            "tool_use_case_count": tool_use_case_count,
            "sealed_test_is_blind": not any("not a confidential blind holdout" in str(blocker).lower() for blocker in blocker_list),
            "disclaimers": scope_disclaimers,
        },
        "benchmark_blockers": blocker_list,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=Path, default=DEFAULT_DATASET)
    parser.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    parser.add_argument("--review-csv", type=Path, default=DEFAULT_REVIEW_CSV)
    parser.add_argument("--output", type=Path, help="Optional path for a persistent audit JSON report.")
    args = parser.parse_args(argv)
    worksheet = args.review_csv if args.review_csv.exists() else None
    try:
        report = audit_dataset(args.dataset, args.schema, worksheet)
        rendered = json.dumps(report, ensure_ascii=False, indent=2)
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(rendered + "\n", encoding="utf-8")
            print(f"audit_report_path={args.output.resolve()}")
        print(f"dataset={report['dataset']['dataset_id']} version={report['dataset']['dataset_version']} cases={report['dataset']['case_count']} sha256={report['dataset']['sha256']}")
        print(f"checks_passed={report['summary']['passed']} checks_failed={report['summary']['failed']} checks_skipped={report['summary']['skipped']} exploratory_ready={str(report['ready_for_exploratory_run']).lower()} benchmark_eligible={str(report['benchmark_eligible']).lower()}")
        if report["summary"]["failed"]:
            for check in report["checks"]:
                if check["status"] == "failed":
                    print(f"failed_check={check['name']}: {'; '.join(check['details'])}", file=sys.stderr)
            return 1
        if not args.output:
            print(rendered)
        return 0
    except (OSError, UnicodeError, json.JSONDecodeError, ValueError) as error:
        print(f"error={error.__class__.__name__}: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
