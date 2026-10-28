#!/usr/bin/env python3
"""Fixed-upstream loopback bridge; application and monitoring containers stay isolated."""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread
import urllib.error
import urllib.request


UPSTREAMS = {
    18083: "http://backend:18083",
    9090: "http://prometheus:9090",
    9093: "http://alertmanager:9093",
}
HOP_BY_HOP = {
    "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
    "te", "trailer", "transfer-encoding", "upgrade", "host",
}
MAX_REQUEST_BYTES = 8 * 1024 * 1024


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def _forward(self):
        if not self.path.startswith("/") or self.path.startswith("//"):
            self.send_error(400)
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length < 0 or length > MAX_REQUEST_BYTES:
            self.send_error(413)
            return
        body = self.rfile.read(length) if length else None
        target = UPSTREAMS[self.server.server_port] + self.path
        headers = {key: value for key, value in self.headers.items()
                   if key.lower() not in HOP_BY_HOP}
        headers["X-Forwarded-For"] = self.client_address[0]
        req = urllib.request.Request(target, data=body, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                self._respond(response.status, response.headers, response.read())
        except urllib.error.HTTPError as error:
            try:
                self._respond(error.code, error.headers, error.read())
            finally:
                error.close()
        except Exception:
            self.send_error(502, "Fixed local upstream unavailable")

    def _respond(self, status, headers, body):
        self.send_response(status)
        for key, value in headers.items():
            if key.lower() not in HOP_BY_HOP and key.lower() != "content-length":
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)
        self.close_connection = True

    def do_GET(self): self._forward()
    def do_HEAD(self): self._forward()
    def do_POST(self): self._forward()
    def do_PUT(self): self._forward()
    def do_PATCH(self): self._forward()
    def do_DELETE(self): self._forward()
    def do_OPTIONS(self): self._forward()

    def log_message(self, fmt, *args):
        print("local-proxy: " + fmt % args, flush=True)


def serve(port):
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()


if __name__ == "__main__":
    servers = []
    for listen_port in UPSTREAMS:
        thread = Thread(target=serve, args=(listen_port,), daemon=True)
        thread.start()
        servers.append(thread)
    for thread in servers:
        thread.join()
