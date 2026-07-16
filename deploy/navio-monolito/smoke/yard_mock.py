import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

EXPECTED_SERVICE_KEY = os.environ.get("CLOUDPORT_INTERNAL_SERVICE_KEY", "")


class YardMockHandler(BaseHTTPRequestHandler):

    def do_GET(self) -> None:
        request_url = urlparse(self.path)

        if request_url.path == "/health":
            self._json_response(200, {"status": "UP"})
            return

        if request_url.path == "/yard/patio/work-queues":
            if not EXPECTED_SERVICE_KEY or self.headers.get("X-CloudPort-Service-Key") != EXPECTED_SERVICE_KEY:
                self._json_response(401, {"erro": "credencial interna ausente ou invalida"})
                return

            visita_ids = parse_qs(request_url.query).get("visitaNavioId", [])
            if len(visita_ids) != 1 or not visita_ids[0].isdigit():
                self._json_response(400, {"erro": "visitaNavioId obrigatorio"})
                return

            self._json_response(200, [])
            return

        self._json_response(404, {"erro": "rota nao simulada"})

    def _json_response(self, status: int, payload: object) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format_string: str, *args: object) -> None:
        print("yard-smoke - " + (format_string % args), flush=True)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8081), YardMockHandler)
    print("yard-smoke escutando na porta 8081", flush=True)
    server.serve_forever()
