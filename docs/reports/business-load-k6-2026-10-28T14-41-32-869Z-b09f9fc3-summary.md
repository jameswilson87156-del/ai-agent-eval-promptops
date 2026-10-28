# Local k6 load report

- Run ID: `k6-2026-10-28T14-41-32-869Z-b09f9fc3`
- Gate: **PASSED**
- Mode: `local-synthetic-load`
- Target: PromptOps isolated synthetic order/refund API
- Source JSON: `business-load-k6-2026-10-28T14-41-32-869Z-b09f9fc3.json`
- Boundary: local synthetic service; this is not production traffic, SLO, or production capacity evidence.

## Measured totals

| Workflows | HTTP requests | Checks passed / failed | HTTP failure rate | Workflow success | Dropped | Active VU peak | VU capacity |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 8103 | 40515 | 81030 / 0 | 0.0000% | 100.0000% | 0 | 1 | 120 |

## Scenario gates

| Scenario | Target workflows/s | Minimum workflows | Measured | Checks passed / failed | HTTP failure | Workflow success | Dropped | Gate |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| low_rate | 5 | 300 | 301 | 3010 / 0 | 0.0000% | 100.0000% | 0 | PASSED |
| medium_rate | 10 | 600 | 601 | 6010 / 0 | 0.0000% | 100.0000% | 0 | PASSED |
| high_rate | 20 | 1200 | 1200 | 12000 / 0 | 0.0000% | 100.0000% | 0 | PASSED |
| stress_rate | 100 | 6000 | 6001 | 60010 / 0 | 0.0000% | 100.0000% | 0 | PASSED |

## Combined latency observations

- HTTP request duration: p50 0.409 ms, p95 1.361 ms, p99 2.833 ms.
- Full workflow duration: p95 5.913 ms, p99 8.744 ms.
- These percentiles combine all four stages and are observations from this local run; no service SLO was defined.
