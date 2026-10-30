# Local k6 load report

- Run ID: `k6-vu-2026-10-30T04-16-16-123Z-94780d55`
- Gate: **PASSED**
- Mode: `local-synthetic-vu-stress`
- Target: PromptOps isolated synthetic order/refund API
- Source JSON: `business-vu-load-k6-vu-2026-10-30T04-16-16-123Z-94780d55.json`
- Boundary: local Docker/H2 only; not production traffic or capacity evidence

## Measured totals

| Workflows | HTTP requests | Checks passed / failed | HTTP failure rate | Workflow success | Dropped | Active VU peak | VU capacity |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 4112 | 20560 | 41120 / 0 | 0.0000% | 100.0000% | 0 | 50 | 50 |


## Closed-loop VU ramp

- Executor: `ramping-vus`; target peak: 50 active VUs.
- Client think time between complete workflows: 1 s.

| Stage | Duration | Target active VUs |
|---|---:|---:|
| ramp to 10 VUs | 20s | 10 |
| hold 10 VUs | 30s | 10 |
| ramp to 25 VUs | 20s | 25 |
| hold 25 VUs | 30s | 25 |
| ramp to 50 VUs | 20s | 50 |
| hold 50 VUs | 30s | 50 |
| ramp down | 15s | 0 |

## Workflow quality gate

| Checks passed / failed | HTTP failure | Workflow success | Business failures | Dropped | Gate |
|---:|---:|---:|---:|---:|---|
| 41120 / 0 | 0.0000% | 100.0000% | 0 | 0 | PASSED |


## Combined latency observations

- HTTP request duration: p50 0.558 ms, p95 2.600 ms, p99 5.587 ms.
- Full workflow duration: p95 11.304 ms, p99 20.070 ms.
- These percentiles are observations from this local closed-loop ramp; no service SLO was defined.
