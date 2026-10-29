# Model Evaluation Human Review Worksheet

- Source dataset: `dataset.v0.2.0.json`
- Dataset version: `0.2.0`
- Dataset SHA-256 captured in every CSV row (file bytes with CRLF normalized to LF): `798d9212cc278112c92a8937db28c95bb4982464267dd5500bebf11ca1862987`
- Rows: 40
- Scope: local synthetic refund-policy classification only. No production/customer data; this is not a general Agent/tool-use benchmark.

## Review each row

1. Read the complete `input_turns_json` and compare the request with the synthetic rules in the prompt and the referenced source files.
2. Decide whether `expected_output_json`, every `deterministic_checks_json` expectation, and the listed success/forbidden outcomes are correct and unambiguous.
3. Set `review_decision` to `approve`, `revise`, or `reject`; fill `reviewer`, `reviewed_at_utc` in ISO-8601 UTC, and `review_notes` with a brief rationale.
4. For `revise`, record the corrected expected behavior in the notes. Apply approved corrections to a new dataset version; do not edit the currently executed v0.2.0 file in place.

`provenance_approved=false` is copied from the dataset and is not an approval. Blank review fields mean unreviewed. This CSV is a review copy and does not change the runnable dataset or make any report Benchmark-eligible.

## Scope questions found during source comparison

- This v0.2.0 set asks an LLM to classify synthetic refund scenarios into a JSON policy decision. It is not a direct test of the refund API's HTTP responses; `available_tools` is empty and no refund operation is executed.
- `model-022` expects the classifier reason code `unauthorized` for a customer who is not the owner. The local API's `BusinessOrderService.requireOrderAccess` calls `requireOwner`, which deliberately returns HTTP 404 for a non-owner. These can both be intentional at different layers, but the reviewer must confirm that the dataset is testing a policy classification rather than exact API response parity.
- `model-017` (missing order status) and `model-020` (conflicting paid totals from two sources) are synthetic classification edge cases. The current API's order record has a single status and paid-total field; these scenarios are not direct replicas of current API storage states.
- `model-037` is multi-turn: it includes an earlier amount, an assistant response, and a later user correction. Review the full transcript and confirm the expected amount follows the latest trusted user turn rather than the earlier request or assistant response.
- If exact API conformance is the intended target, revise scope and expected outputs to map to the actual HTTP contract. If policy classification is the target, retain the JSON reason-code contract and describe these as synthetic classification scenarios. Do not mix the two interpretations in one reported metric.
