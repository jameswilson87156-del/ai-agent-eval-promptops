import importlib.util
import json
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("audit_dataset.py")
SPEC = importlib.util.spec_from_file_location("audit_dataset", MODULE_PATH)
audit_module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(audit_module)

HERE = Path(__file__).parent
DATASET_PATH = HERE / "dataset.v0.2.0.json"
SCHEMA_PATH = HERE / "case.v0.2.0.schema.json"
WORKSHEET_PATH = HERE / "human-review-v0.2.0.csv"


def load_source_dataset():
    return json.loads(DATASET_PATH.read_text(encoding="utf-8"))


def write_json(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def test_current_dataset_audit_is_exploratory_ready_but_never_benchmark_eligible():
    report = audit_module.audit_dataset(DATASET_PATH, SCHEMA_PATH, WORKSHEET_PATH)

    assert report["summary"]["failed"] == 0
    assert report["ready_for_exploratory_run"] is True
    assert report["benchmark_eligible"] is False
    assert report["dataset"]["case_count"] == 40
    assert report["dataset"]["split_counts"] == {
        "development": 24,
        "validation": 8,
        "sealed_test": 8,
    }
    assert report["human_review"]["approved_case_count"] == 0
    assert report["human_review"]["pending_case_count"] == 40
    assert report["scope"]["tool_use_evaluation"] is False
    assert report["scope"]["sealed_test_is_blind"] is False
    assert report["dataset"]["sha256"]


def test_missing_schema_validation_cannot_claim_exploratory_readiness():
    report = audit_module.audit_dataset(DATASET_PATH)
    schema_check = next(check for check in report["checks"] if check["name"] == "case_json_schema")

    assert schema_check["status"] == "skipped"
    assert report["ready_for_exploratory_run"] is False


def test_duplicate_ids_and_family_splits_fail_audit(tmp_path):
    dataset = load_source_dataset()
    dataset["cases"][1]["case_id"] = dataset["cases"][0]["case_id"]
    dataset["cases"][1]["scenario_family"] = dataset["cases"][0]["scenario_family"]
    dataset_path = write_json(tmp_path / "dataset.json", dataset)

    report = audit_module.audit_dataset(dataset_path, SCHEMA_PATH)
    failed_names = {check["name"] for check in report["checks"] if check["status"] == "failed"}

    assert report["ready_for_exploratory_run"] is False
    assert "unique_case_ids" in failed_names
    assert "unique_scenario_families" in failed_names


def test_blank_identity_and_unknown_split_fail_closed(tmp_path):
    dataset = load_source_dataset()
    dataset["cases"][0]["case_id"] = ""
    dataset["cases"][0]["split"] = "hidden"
    dataset_path = write_json(tmp_path / "dataset.json", dataset)

    report = audit_module.audit_dataset(dataset_path, SCHEMA_PATH)
    failed_names = {check["name"] for check in report["checks"] if check["status"] == "failed"}

    assert report["ready_for_exploratory_run"] is False
    assert "unique_case_ids" in failed_names
    assert "split_counts_match_metadata" in failed_names


def test_golden_and_business_rule_check_must_agree(tmp_path):
    dataset = load_source_dataset()
    dataset["cases"][0]["expected_output"]["allowed"] = False
    dataset_path = write_json(tmp_path / "dataset.json", dataset)

    report = audit_module.audit_dataset(dataset_path, SCHEMA_PATH)
    failed = [check for check in report["checks"] if check["name"] == "golden_matches_deterministic_checks"]

    assert report["ready_for_exploratory_run"] is False
    assert failed[0]["status"] == "failed"
    assert "model-001" in " ".join(failed[0]["details"])


def test_canary_input_requires_a_matching_negative_output_assertion(tmp_path):
    dataset = load_source_dataset()
    case = next(item for item in dataset["cases"] if item["case_id"] == "model-034")
    case["deterministic_checks"] = [
        check for check in case["deterministic_checks"] if check["check_id"] != "canary_not_echoed"
    ]
    dataset_path = write_json(tmp_path / "dataset.json", dataset)

    report = audit_module.audit_dataset(dataset_path, SCHEMA_PATH)
    failed = [check for check in report["checks"] if check["name"] == "prompt_injection_canary_guard"]

    assert report["ready_for_exploratory_run"] is False
    assert failed[0]["status"] == "failed"
    assert "model-034" in " ".join(failed[0]["details"])


def test_review_worksheet_must_match_dataset_hash_and_case_ids(tmp_path):
    dataset = load_source_dataset()
    dataset_path = write_json(tmp_path / "dataset.json", dataset)
    worksheet = WORKSHEET_PATH.read_text(encoding="utf-8-sig")
    worksheet = worksheet.replace("model-040", "model-999", 1)
    worksheet_path = tmp_path / "review.csv"
    worksheet_path.write_text(worksheet, encoding="utf-8-sig")

    report = audit_module.audit_dataset(dataset_path, SCHEMA_PATH, worksheet_path)
    failed_names = {check["name"] for check in report["checks"] if check["status"] == "failed"}

    assert report["ready_for_exploratory_run"] is False
    assert "review_worksheet_matches_dataset" in failed_names
