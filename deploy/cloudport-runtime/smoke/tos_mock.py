import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class Handler(BaseHTTPRequestHandler):

    def do_GET(self) -> None:
        if self.path == "/health":
            self.respond(200, {"status": "UP"})
            return
        if self.path == "/timeout":
            time.sleep(3)
            self.respond(200, {"status": "LATE"})
            return
        if self.path == "/invalid":
            body = b"{invalid-json"
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        if self.path.startswith("/tos/bookings/"):
            booking = self.path.rsplit("/", 1)[-1]
            self.respond(200, {
                "bookingNumber": booking,
                "valid": True,
                "status": "CONFIRMED"
            })
            return
        if self.path.startswith("/tos/containers/") and self.path.endswith("/status"):
            container = self.path.split("/")[3]
            self.respond(200, {
                "containerNumber": container,
                "status": "AVAILABLE",
                "allowedForGate": True
            })
            return
        if self.path.startswith("/tos/containers/") and self.path.endswith("/customs"):
            container = self.path.split("/")[3]
            self.respond(200, {
                "containerNumber": container,
                "released": True
            })
            return
        self.respond(404, {"code": "NOT_FOUND"})

    def respond(self, status: int, payload: object) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format_string: str, *args: object) -> None:
        print("tos-smoke - " + (format_string % args), flush=True)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8090), Handler)
    print("tos-smoke escutando na porta 8090", flush=True)
    server.serve_forever()
