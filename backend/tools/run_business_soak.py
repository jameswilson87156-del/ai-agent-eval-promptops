#!/usr/bin/env python3
"""Repeated local order/refund workflow for bounded soak verification."""

import argparse
import json
import os
import signal
import socket
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path


STOP_REQUESTED = False


def now_utc():
    return datetime.now(timezone.utc).isoformat()


def request(base_url, path, method="GET", payload=None, headers=None, timeout=10):
    body = None if payload is None else json.dumps(payload).encode("utf-8")
    request_headers = {"Accept": "application/json"}
    if body is not None:
        request_headers["Content-Type"] = "application/json"
    request_headers.update(headers or {})
    req = urllib.request.Request(base_url + path, data=body, headers=request_headers, method=method)
    started = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            raw = response.read()
            elapsed_ms = round((time.monotonic() - started) * 1000, 3)
            try:
                parsed = json.loads(raw.decode("utf-8")) if raw else None
            except json.JSONDecodeError:
                return response.status, None, elapsed_ms, "invalid_json"
            return response.status, parsed, elapsed_ms, None
    except urllib.error.HTTPError as error:
        elapsed_ms = round((time.monotonic() - started) * 1000, 3)
        error.close()
        return error.code, None, elapsed_ms, "http_error"
    except Exception as error:  # Keep error evidence useful without recording request bodies.
        elapsed_ms = round((time.monotonic() - started) * 1000, 3)
        return None, None, elapsed_ms, type(error).__name__


def require_loopback(url, allow_compose_service=False):
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "http" or not parsed.hostname:
        raise ValueError("base URL must be an http:// loopback address")
    if parsed.username or parsed.password or parsed.path or parsed.query or parsed.fragment:
        raise ValueError("base URL must not contain credentials, a path, query, or fragment")
    if allow_compose_service and parsed.hostname == "backend" and parsed.port == 18083:
        return f"{parsed.scheme}://{parsed.netloc}"
    try:
        addresses = {entry[4][0] for entry in socket.getaddrinfo(parsed.hostname, parsed.port or 80, type=socket.SOCK_STREAM)}
    except socket.gaierror as error:
        raise ValueError("soak runner only allows a loopback target") from error
    if not addresses:
        raise ValueError("could not resolve a loopback address")
    if any(not socket_address_is_loopback(address) for address in addresses):
        raise ValueError("soak runner only allows a loopback target")
    return f"{parsed.scheme}://{parsed.netloc}"


def socket_address_is_loopback(address):
    import ipaddress
    try:
        return ipaddress.ip_address(address).is_loopback
    except ValueError:
        return False


def write_report(path, report, initial=False):
    encoded = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    if initial:
        with path.open("x", encoding="utf-8") as stream:
            stream.write(encoded)
        return
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(encoded, encoding="utf-8")
    os.replace(temporary, path)


def call(base_url, path, method, payload, step, record, idempotency_key=None, timeout=10):
    headers = {"X-Demo-Actor-Id": "soak-synthetic-customer", "X-Demo-Role": "customer"}
    if idempotency_key:
        headers["Idempotency-Key"] = idempotency_key
    status, response, elapsed_ms, error = request(base_url, path, method, payload, headers, timeout)
    record["requests"].append({"step": step, "status": status, "latency_ms": elapsed_ms, "error": error})
    return status, response


def run_workflow(base_url, sequence, timeout):
    run_id = f"soak-{sequence:06d}-{uuid.uuid4().hex[:12]}"
    record = {"sequence": sequence, "workflow_id": run_id, "started_at": now_utc(), "requests": [], "passed": False}
    status, created = call(base_url, "/api/business/orders", "POST",
        {"amount": 99.00, "currency": "CNY", "itemName": "Synthetic soak-test order"}, "order_create", record,
        f"{run_id}-create", timeout)
    order_id = created.get("orderId") if isinstance(created, dict) else None
    if status != 201 or not order_id or created.get("status") != "PENDING_PAYMENT":
        record["failure"] = "order_create_assertion"
        record["finished_at"] = now_utc()
        return record

    escaped = urllib.parse.quote(order_id, safe="")
    status, paid = call(base_url, f"/api/business/orders/{escaped}/payments", "POST", {"amount": 99.00},
        "payment_create", record, f"{run_id}-payment", timeout)
    if status != 201 or not isinstance(paid, dict) or paid.get("order", {}).get("status") != "PAID":
        record["failure"] = "payment_assertion"
        record["finished_at"] = now_utc()
        return record

    status, current = call(base_url, f"/api/business/orders/{escaped}", "GET", None, "order_read_paid", record, timeout=timeout)
    if status != 200 or not isinstance(current, dict) or current.get("status") != "PAID":
        record["failure"] = "paid_order_read_assertion"
        record["finished_at"] = now_utc()
        return record

    status, refunded = call(base_url, f"/api/business/orders/{escaped}/refunds", "POST",
        {"amount": 99.00, "reason": "Synthetic soak-test refund"}, "refund_create", record,
        f"{run_id}-refund", timeout)
    if status != 201 or not isinstance(refunded, dict) or refunded.get("order", {}).get("status") != "REFUNDED":
        record["failure"] = "refund_assertion"
        record["finished_at"] = now_utc()
        return record

    status, current = call(base_url, f"/api/business/orders/{escaped}", "GET", None, "order_read_refunded", record, timeout=timeout)
    record["passed"] = status == 200 and isinstance(current, dict) and current.get("status") == "REFUNDED"
    if not record["passed"]:
        record["failure"] = "refunded_order_read_assertion"
    record["order_id"] = order_id
    record["finished_at"] = now_utc()
    return record


def handle_stop(_signum, _frame):
    global STOP_REQUESTED
    STOP_REQUESTED = True


def main():
    global STOP_REQUESTED
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:18083")
    parser.add_argument("--duration-minutes", type=float, default=30)
    parser.add_argument("--interval-seconds", type=float, default=30)
    parser.add_argument("--request-timeout-seconds", type=float, default=10)
    parser.add_argument("--output", help="exclusive JSON output path; default creates a unique file in PROMPTOPS_EVIDENCE_DIR or the current directory")
    parser.add_argument("--allow-compose-service", action="store_true",
                        help="allow only the isolated Compose service backend:18083 target")
    args = parser.parse_args()
    if args.duration_minutes <= 0 or args.duration_minutes > 720:
        parser.error("duration must be > 0 and <= 720 minutes")
    if args.interval_seconds <= 0 or args.interval_seconds > 3600:
        parser.error("interval must be > 0 and <= 3600 seconds")
    if args.request_timeout_seconds <= 0 or args.request_timeout_seconds > 120:
        parser.error("request timeout must be > 0 and <= 120 seconds")

    base_url = require_loopback(args.base_url.rstrip("/"), args.allow_compose_service)
    if args.output:
        output = Path(args.output).resolve()
    else:
        evidence_dir = Path(os.environ.get("PROMPTOPS_EVIDENCE_DIR", ".")).resolve()
        output = evidence_dir / f"business-soak-{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')}.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    report = {
        "report_schema_version": "1.0",
        "test_type": "local_business_api_soak",
        "target_scope": "local synthetic order/refund API; no production system or payment gateway",
        "target_access_mode": "isolated_compose_service" if urllib.parse.urlparse(base_url).hostname == "backend" else "loopback",
        "base_url": base_url,
        "configured_duration_minutes": args.duration_minutes,
        "configured_interval_seconds": args.interval_seconds,
        "started_at": now_utc(),
        "state": "RUNNING",
        "iterations": [],
    }
    if output.exists():
        raise FileExistsError(f"refusing to overwrite existing evidence: {output}")
    write_report(output, report, initial=True)

    signal.signal(signal.SIGINT, handle_stop)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, handle_stop)
    deadline = time.monotonic() + args.duration_minutes * 60
    sequence = 0
    state = "COMPLETED"
    try:
        while time.monotonic() < deadline and not STOP_REQUESTED:
            loop_started = time.monotonic()
            sequence += 1
            health_status, health, health_ms, health_error = request(base_url, "/actuator/health", timeout=args.request_timeout_seconds)
            iteration = run_workflow(base_url, sequence, args.request_timeout_seconds)
            iteration["health"] = {
                "status": health_status,
                "latency_ms": health_ms,
                "reported_status": health.get("status") if isinstance(health, dict) else None,
                "error": health_error,
            }
            iteration["passed"] = iteration["passed"] and health_status == 200 and isinstance(health, dict) and health.get("status") == "UP"
            if not iteration["passed"] and "failure" not in iteration:
                iteration["failure"] = "health_assertion"
            report["iterations"].append(iteration)
            report["last_update_at"] = now_utc()
            report["summary"] = {
                "iterations": len(report["iterations"]),
                "passed": sum(1 for item in report["iterations"] if item["passed"]),
                "failed": sum(1 for item in report["iterations"] if not item["passed"]),
            }
            write_report(output, report)
            remaining = max(0, min(args.interval_seconds - (time.monotonic() - loop_started), deadline - time.monotonic()))
            if remaining > 0 and not STOP_REQUESTED:
                time.sleep(remaining)
    except KeyboardInterrupt:
        STOP_REQUESTED = True
    except Exception as error:
        state = "ERROR"
        report["fatal_error"] = type(error).__name__
        report["error_message"] = str(error)[:240]
    finally:
        if STOP_REQUESTED and state == "COMPLETED":
            state = "INTERRUPTED"
        report["state"] = state
        report["completed_at"] = now_utc()
        report["elapsed_seconds"] = round((datetime.fromisoformat(report["completed_at"]) - datetime.fromisoformat(report["started_at"])).total_seconds(), 3)
        report["summary"] = {
            "iterations": len(report["iterations"]),
            "passed": sum(1 for item in report["iterations"] if item["passed"]),
            "failed": sum(1 for item in report["iterations"] if not item["passed"]),
            "requests": sum(len(item["requests"]) + 1 for item in report["iterations"]),
        }
        write_report(output, report)
    print(json.dumps(report["summary"] | {"state": state, "evidence": str(output)}, ensure_ascii=False))
    return 0 if state == "COMPLETED" and report["summary"]["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
