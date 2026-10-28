import sys
from pathlib import Path

import pytest


TOOLS_PATH = Path(__file__).resolve().parents[1] / "tools"
sys.path.insert(0, str(TOOLS_PATH))

from check_agent_eval_stability import (  # noqa: E402
    DATASET_PATH,
    percentile,
    stable_signature,
    validate_loopback_url,
)


def test_sampler_loads_the_committed_fixed_dataset():
    assert DATASET_PATH.is_file()


@pytest.mark.parametrize(
    "url",
    ["http://localhost:18080", "http://127.0.0.1:18080", "http://[::1]:18080"],
)
def test_sampler_allows_only_loopback_targets(url):
    assert validate_loopback_url(url).startswith("http://")


@pytest.mark.parametrize(
    "url",
    [
        "https://example.com",
        "http://192.168.1.12:18080",
        "http://localhost:18080/path?x=1",
        "http://localhost:18080/api",
        "http://localhost:not-a-port",
    ],
)
def test_sampler_rejects_non_loopback_or_ambiguous_targets(url):
    with pytest.raises(ValueError):
        validate_loopback_url(url)


def test_nearest_rank_percentile_is_deterministic():
    assert percentile([8.0, 1.0, 5.0, 2.0], 0.95) == 8.0


def test_stability_signature_ignores_volatile_trace_metadata():
    base = {
        "cases": [
            {
                "case_id": "agent-001",
                "execution_status": "success",
                "case_status": "passed",
                "actual_output": {"final_output": "hello"},
                "assertion_results": [
                    {
                        "path": "trace",
                        "operator": "ordered_events",
                        "passed": True,
                        "actual": [
                            {
                                "event_id": "first",
                                "timestamp": "now-a",
                                "duration_ms": 0.1,
                                "event_type": "run_completed",
                                "status": "success",
                            }
                        ],
                    }
                ],
                "execution_steps": [{"event_id": "first"}],
            }
        ]
    }
    repeated = {
        "cases": [
            {
                "case_id": "agent-001",
                "execution_status": "success",
                "case_status": "passed",
                "actual_output": {"final_output": "hello"},
                "assertion_results": [
                    {
                        "path": "trace",
                        "operator": "ordered_events",
                        "passed": True,
                        "actual": [
                            {
                                "event_id": "second",
                                "timestamp": "now-b",
                                "duration_ms": 0.2,
                                "event_type": "run_completed",
                                "status": "success",
                            }
                        ],
                    }
                ],
                "execution_steps": [{"event_id": "second"}],
            }
        ]
    }
    assert stable_signature(base) == stable_signature(repeated)
