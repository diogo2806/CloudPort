import json
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

EXPECTED_SERVICE_KEY = os.environ.get("CLOUDPORT_INTERNAL_SERVICE_KEY", "")
ORDERS_BY_ITEM: dict[int, dict[str, object]] = {}
NEXT_ORDER_ID = 7000


class YardMockHandler(BaseHTTPRequestHandler):

    def do_GET(self) -> None:
        request_url = urlparse(self.path)

        if request_url.path == "/health":
            self._json_response(200, {"status": "UP"})
            return

        if request_url.path.startswith("/yard/patio/") and not self._authorized_internal_request():
            return

        if request_url.path == "/yard/patio/posicoes":
            self._json_response(200, [{
                "id": 101,
                "linha": 1,
                "coluna": 2,
                "camadaOperacional": "3",
                "ocupada": False,
                "codigoConteiner": None,
                "statusConteiner": None,
            }])
            return

        if request_url.path == "/yard/patio/work-queues":
            visita_id = self._single_numeric_query(request_url, "visitaNavioId")
            if visita_id is None:
                return
            ordens = self._orders_for_visit(visita_id)
            if not ordens:
                self._json_response(200, [])
                return
            self._json_response(200, [{
                "id": 9001,
                "identificador": f"WQ-SMOKE-{visita_id}",
                "agrupamento": "BLOCO-SMOKE",
                "visitaNavioId": visita_id,
                "berco": "BERCO-1",
                "porao": 1,
                "blocoZona": "BLOCO-SMOKE",
                "sequenciaInicial": 1,
                "pow": "POW-SMOKE",
                "poolOperacional": "POOL-SMOKE",
                "equipamento": "RTG-SMOKE-01",
                "status": "ATIVA",
                "prioridadeOperacional": 1,
                "totalOrdens": len(ordens),
                "jobList": ordens,
                "criadoEm": "2026-07-16T09:00:00",
                "atualizadoEm": "2026-07-16T09:00:00",
            }])
            return

        ordens_match = re.fullmatch(r"/yard/patio/ordens/visita-navio/(\d+)", request_url.path)
        if ordens_match:
            visita_id = int(ordens_match.group(1))
            ordens = self._orders_for_visit(visita_id)
            concluidas = [dict(ordem, statusOrdem="CONCLUIDA") for ordem in ordens]
            self._json_response(200, concluidas)
            return

        filas_match = re.fullmatch(r"/yard/patio/ordens/visita-navio/(\d+)/filas", request_url.path)
        if filas_match:
            visita_id = int(filas_match.group(1))
            ordens = self._orders_for_visit(visita_id)
            self._json_response(200, [] if not ordens else [{
                "identificador": f"WQ-SMOKE-{visita_id}",
                "agrupamento": "BLOCO-SMOKE",
                "visitaNavioId": visita_id,
                "berco": "BERCO-1",
                "blocoZona": "BLOCO-SMOKE",
                "sequenciaInicial": 1,
                "status": "ATIVA",
                "totalOrdens": len(ordens),
                "ordens": ordens,
            }])
            return

        sem_cobertura_match = re.fullmatch(
            r"/yard/patio/ordens/visita-navio/(\d+)/sem-cobertura",
            request_url.path,
        )
        if sem_cobertura_match:
            self._json_response(200, [])
            return

        self._json_response(404, {"erro": "rota nao simulada"})

    def do_POST(self) -> None:
        global NEXT_ORDER_ID
        request_url = urlparse(self.path)
        if request_url.path.startswith("/yard/patio/") and not self._authorized_internal_request():
            return

        if request_url.path == "/yard/patio/ordens/navio":
            body = self._read_json_body()
            if body is None:
                return
            item_id = body.get("itemOperacaoNavioId")
            visita_id = body.get("visitaNavioId")
            if not isinstance(item_id, int) or not isinstance(visita_id, int):
                self._json_response(400, {"erro": "visitaNavioId e itemOperacaoNavioId obrigatorios"})
                return
            existing = ORDERS_BY_ITEM.get(item_id)
            if existing is not None:
                self._json_response(200, existing)
                return

            NEXT_ORDER_ID += 1
            order = {
                "id": NEXT_ORDER_ID,
                "codigoConteiner": body.get("codigoConteiner"),
                "destino": body.get("destino"),
                "linhaDestino": body.get("linhaDestino"),
                "colunaDestino": body.get("colunaDestino"),
                "camadaDestino": body.get("camadaDestino"),
                "tipoMovimento": body.get("tipoMovimento"),
                "statusOrdem": "PENDENTE",
                "visitaNavioId": visita_id,
                "itemOperacaoNavioId": item_id,
                "sequenciaNavio": body.get("sequenciaNavio"),
                "prioridadeOperacional": body.get("prioridadeOperacional"),
            }
            ORDERS_BY_ITEM[item_id] = order
            self._json_response(201, order)
            return

        self._json_response(404, {"erro": "rota nao simulada"})

    def _authorized_internal_request(self) -> bool:
        if not EXPECTED_SERVICE_KEY or self.headers.get("X-CloudPort-Service-Key") != EXPECTED_SERVICE_KEY:
            self._json_response(401, {"erro": "credencial interna ausente ou invalida"})
            return False
        if not self.headers.get("X-Correlation-Id"):
            self._json_response(400, {"erro": "correlation id ausente"})
            return False
        if not self.headers.get("traceparent"):
            self._json_response(400, {"erro": "traceparent ausente"})
            return False
        return True

    def _single_numeric_query(self, request_url, name: str) -> int | None:
        values = parse_qs(request_url.query).get(name, [])
        if len(values) != 1 or not values[0].isdigit():
            self._json_response(400, {"erro": f"{name} obrigatorio"})
            return None
        return int(values[0])

    def _orders_for_visit(self, visita_id: int) -> list[dict[str, object]]:
        return [
            order for order in ORDERS_BY_ITEM.values()
            if order.get("visitaNavioId") == visita_id
        ]

    def _read_json_body(self) -> dict[str, object] | None:
        try:
            content_length = int(self.headers.get("Content-Length", "0"))
            payload = self.rfile.read(content_length) if content_length > 0 else b"{}"
            value = json.loads(payload.decode("utf-8"))
            if not isinstance(value, dict):
                raise ValueError("body deve ser objeto")
            return value
        except (ValueError, json.JSONDecodeError) as error:
            self._json_response(400, {"erro": f"json invalido: {error}"})
            return None

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
