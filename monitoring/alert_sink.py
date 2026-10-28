#!/usr/bin/env python3
"""Local-only Alertmanager webhook sink that stores a sanitized evidence trail."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path


LOG_PATH = Path(os.environ.get("ALERT_LOG_PATH", "/evidence/alert-events.jsonl"))


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/alerts":
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length < 1 or length > 1_000_000:
                self.send_error(413)
                return
            payload = json.loads(self.rfile.read(length))
            alerts = []
            for alert in payload.get("alerts", []):
                labels = alert.get("labels", {})
                annotations = alert.get("annotations", {})
                alerts.append({
                    "status": str(alert.get("status", "unknown"))[:24],
                    "alertname": str(labels.get("alertname", "unknown"))[:120],
                    "severity": str(labels.get("severity", "unknown"))[:24],
                    "summary": str(annotations.get("summary", ""))[:240],
                    "startsAt": str(alert.get("startsAt", ""))[:40],
                    "endsAt": str(alert.get("endsAt", ""))[:40],
                })
            record = {
                "status": str(payload.get("status", "unknown"))[:24],
                "receiver": str(payload.get("receiver", ""))[:80],
                "alerts": alerts,
            }
            LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
            with LOG_PATH.open("a", encoding="utf-8") as stream:
                stream.write(json.dumps(record, ensure_ascii=False) + "\n")
            body = b'{"stored":true}'
            self.send_response(202)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        except (ValueError, json.JSONDecodeError):
            self.send_error(400)

    def log_message(self, fmt, *args):
        print("alert-sink: " + fmt % args, flush=True)


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8088), Handler).serve_forever()
