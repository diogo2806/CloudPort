#!/usr/bin/env python3
"""Executa carga HTTP e coleta métricas de memória do cloudport-runtime."""

from __future__ import annotations

import argparse
import base64
import concurrent.futures
import csv
import hashlib
import hmac
import json
import math
import os
import pathlib
import random
import statistics
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Any

MIB = 1024 * 1024
DEFAULT_ENDPOINTS = (
    "/actuator/health",
    "/operacao/corte",
    "/yard/status",
    "/v3/api-docs",
    "/navios",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--container", required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--rabbit-enabled", choices=("true", "false"), required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--thresholds", required=True)
    parser.add_argument("--warmup-seconds", type=int, default=15)
    parser.add_argument("--duration-seconds", type=int, default=45)
    parser.add_argument("--concurrency", type=int, default=12)
    parser.add_argument("--sample-interval", type=float, default=2.0)
    parser.add_argument("--timeout-seconds", type=float, default=5.0)
    parser.add_argument("--enforce-thresholds", action="store_true")
    parser.add_argument("--endpoints", nargs="*", default=list(DEFAULT_ENDPOINTS))
    return parser.parse_args()


def b64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def jwt(secret: str) -> str:
    now = int(time.time())
    header = b64url(json.dumps({"alg": "HS256", "typ": "JWT"}, separators=(",", ":")).encode())
    payload = b64url(
        json.dumps(
            {
                "sub": "cloudport-memory-benchmark",
                "roles": ["ADMIN_PORTO", "PLANEJADOR"],
                "iat": now,
                "exp": now + 7200,
            },
            separators=(",", ":"),
        ).encode()
    )
    message = f"{header}.{payload}".encode("ascii")
    signature = hmac.new(secret.encode(), message, hashlib.sha256).digest()
    return f"{header}.{payload}.{b64url(signature)}"


def read_thresholds(path: pathlib.Path) -> dict[str, float]:
    result: dict[str, float] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = float(value.strip())
    return result


def percentile(values: list[float], quantile: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    position = (len(ordered) - 1) * quantile
    lower = math.floor(position)
    upper = math.ceil(position)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def slope_per_minute(samples: list[dict[str, Any]], field: str) -> float:
    points = [(float(item["elapsed_seconds"]), float(item[field]) / MIB) for item in samples]
    if len(points) < 2:
        return 0.0
    mean_x = statistics.fmean(point[0] for point in points)
    mean_y = statistics.fmean(point[1] for point in points)
    denominator = sum((x - mean_x) ** 2 for x, _ in points)
    if denominator == 0:
        return 0.0
    slope_per_second = sum((x - mean_x) * (y - mean_y) for x, y in points) / denominator
    return slope_per_second * 60.0


def growth_percent(samples: list[dict[str, Any]], field: str) -> float:
    if len(samples) < 2:
        return 0.0
    first = float(samples[0][field])
    last = float(samples[-1][field])
    if first <= 0:
        return 0.0
    return ((last - first) / first) * 100.0


def metric_value(base_url: str, metric: str, token: str, tags: list[str] | None, timeout: float) -> float:
    params = [("tag", tag) for tag in tags or []]
    suffix = "?" + urllib.parse.urlencode(params) if params else ""
    request = urllib.request.Request(
        f"{base_url}/actuator/metrics/{metric}{suffix}",
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        payload = json.load(response)
    measurements = payload.get("measurements", [])
    return float(measurements[0]["value"]) if measurements else 0.0


def container_rss_bytes(container: str) -> int:
    pid = subprocess.check_output(
        ["docker", "inspect", "--format", "{{.State.Pid}}", container], text=True
    ).strip()
    status = pathlib.Path(f"/proc/{pid}/status").read_text(encoding="utf-8")
    for line in status.splitlines():
        if line.startswith("VmRSS:"):
            return int(line.split()[1]) * 1024
    raise RuntimeError(f"VmRSS ausente para o container {container}")


@dataclass
class RequestStats:
    total: int = 0
    successful: int = 0
    failed: int = 0
    status_codes: dict[str, int] = field(default_factory=dict)
    latencies_ms: list[float] = field(default_factory=list)
    lock: threading.Lock = field(default_factory=threading.Lock)

    def record(self, status: int, latency_ms: float) -> None:
        with self.lock:
            self.total += 1
            self.status_codes[str(status)] = self.status_codes.get(str(status), 0) + 1
            if 200 <= status < 400:
                self.successful += 1
            else:
                self.failed += 1
            self.latencies_ms.append(latency_ms)

    def snapshot(self) -> dict[str, Any]:
        with self.lock:
            success_percent = self.successful * 100.0 / self.total if self.total else 0.0
            return {
                "total": self.total,
                "successful": self.successful,
                "failed": self.failed,
                "success_percent": success_percent,
                "status_codes": dict(sorted(self.status_codes.items())),
                "latency_ms_p50": percentile(self.latencies_ms, 0.50),
                "latency_ms_p95": percentile(self.latencies_ms, 0.95),
                "latency_ms_max": max(self.latencies_ms, default=0.0),
            }


def perform_request(base_url: str, endpoint: str, token: str, timeout: float) -> tuple[int, float]:
    started = time.monotonic()
    request = urllib.request.Request(
        base_url.rstrip("/") + endpoint,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
            "X-Correlation-Id": "cloudport-memory-benchmark",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response.read(4096)
            status = response.status
    except urllib.error.HTTPError as error:
        error.read(4096)
        status = error.code
    except Exception:
        status = 0
    latency_ms = (time.monotonic() - started) * 1000.0
    return status, latency_ms


def load_worker(stop: threading.Event, args: argparse.Namespace, token: str, stats: RequestStats) -> None:
    endpoints = tuple(args.endpoints)
    while not stop.is_set():
        endpoint = random.choice(endpoints)
        status, latency = perform_request(args.base_url, endpoint, token, args.timeout_seconds)
        stats.record(status, latency)


def collect_sample(args: argparse.Namespace, token: str, elapsed: float, phase: str) -> dict[str, Any]:
    sample: dict[str, Any] = {
        "timestamp_epoch": time.time(),
        "elapsed_seconds": round(elapsed, 3),
        "phase": phase,
        "rss_bytes": container_rss_bytes(args.container),
        "heap_used_bytes": 0.0,
        "heap_committed_bytes": 0.0,
        "heap_max_bytes": 0.0,
        "metaspace_used_bytes": 0.0,
        "direct_used_bytes": 0.0,
        "live_threads": 0.0,
        "loaded_classes": 0.0,
        "metric_errors": [],
    }
    queries = {
        "heap_used_bytes": ("jvm.memory.used", ["area:heap"]),
        "heap_committed_bytes": ("jvm.memory.committed", ["area:heap"]),
        "heap_max_bytes": ("jvm.memory.max", ["area:heap"]),
        "metaspace_used_bytes": ("jvm.memory.used", ["id:Metaspace"]),
        "direct_used_bytes": ("jvm.buffer.memory.used", ["id:direct"]),
        "live_threads": ("jvm.threads.live", None),
        "loaded_classes": ("jvm.classes.loaded", None),
    }
    for field_name, (metric, tags) in queries.items():
        try:
            sample[field_name] = metric_value(args.base_url, metric, token, tags, args.timeout_seconds)
        except Exception as error:  # coleta parcial ainda é útil para diagnóstico
            sample["metric_errors"].append(f"{field_name}:{type(error).__name__}")
    return sample


def summarize(args: argparse.Namespace, samples: list[dict[str, Any]], stats: dict[str, Any]) -> dict[str, Any]:
    measured = [item for item in samples if item["phase"] == "measurement"]

    def values(field: str) -> list[float]:
        return [float(item[field]) for item in measured]

    def memory_summary(field: str) -> dict[str, float]:
        raw = values(field)
        mib = [value / MIB for value in raw]
        return {
            "max_mib": max(mib, default=0.0),
            "p50_mib": percentile(mib, 0.50),
            "p95_mib": percentile(mib, 0.95),
            "last_mib": mib[-1] if mib else 0.0,
            "growth_percent": growth_percent(measured, field),
            "slope_mib_per_min": slope_per_minute(measured, field),
        }

    return {
        "label": args.label,
        "rabbit_enabled": args.rabbit_enabled == "true",
        "warmup_seconds": args.warmup_seconds,
        "duration_seconds": args.duration_seconds,
        "concurrency": args.concurrency,
        "sample_count": len(measured),
        "requests": stats,
        "memory": {
            "rss": memory_summary("rss_bytes"),
            "heap_used": memory_summary("heap_used_bytes"),
            "heap_committed": memory_summary("heap_committed_bytes"),
            "heap_max": memory_summary("heap_max_bytes"),
            "metaspace_used": memory_summary("metaspace_used_bytes"),
            "direct_used": memory_summary("direct_used_bytes"),
        },
        "threads": {
            "max": max(values("live_threads"), default=0.0),
            "p95": percentile(values("live_threads"), 0.95),
            "last": values("live_threads")[-1] if measured else 0.0,
        },
        "classes": {
            "max": max(values("loaded_classes"), default=0.0),
            "last": values("loaded_classes")[-1] if measured else 0.0,
        },
        "metric_error_count": sum(len(item["metric_errors"]) for item in measured),
    }


def evaluate(summary: dict[str, Any], thresholds: dict[str, float]) -> list[str]:
    failures: list[str] = []
    checks = (
        (summary["memory"]["rss"]["max_mib"], thresholds["MAX_RSS_MIB"], "RSS máximo", "MiB"),
        (summary["memory"]["heap_used"]["max_mib"], thresholds["MAX_HEAP_USED_MIB"], "heap usado máximo", "MiB"),
        (summary["memory"]["metaspace_used"]["max_mib"], thresholds["MAX_METASPACE_USED_MIB"], "metaspace máximo", "MiB"),
        (summary["memory"]["direct_used"]["max_mib"], thresholds["MAX_DIRECT_USED_MIB"], "memória direta máxima", "MiB"),
        (summary["threads"]["max"], thresholds["MAX_LIVE_THREADS"], "threads vivas", ""),
        (summary["memory"]["rss"]["slope_mib_per_min"], thresholds["MAX_RSS_SLOPE_MIB_PER_MIN"], "inclinação de RSS", "MiB/min"),
        (summary["memory"]["heap_used"]["slope_mib_per_min"], thresholds["MAX_HEAP_SLOPE_MIB_PER_MIN"], "inclinação de heap", "MiB/min"),
    )
    for actual, limit, description, unit in checks:
        if actual > limit:
            failures.append(f"{description}: {actual:.2f} {unit} > {limit:.2f} {unit}".strip())
    if summary["requests"]["success_percent"] < thresholds["MIN_SUCCESS_PERCENT"]:
        failures.append(
            f"requisições com sucesso: {summary['requests']['success_percent']:.2f}% "
            f"< {thresholds['MIN_SUCCESS_PERCENT']:.2f}%"
        )
    if summary["metric_error_count"]:
        failures.append(f"coleta Actuator incompleta: {summary['metric_error_count']} erros")
    return failures


def write_outputs(output: pathlib.Path, samples: list[dict[str, Any]], summary: dict[str, Any], failures: list[str]) -> None:
    output.mkdir(parents=True, exist_ok=True)
    fields = [
        "timestamp_epoch",
        "elapsed_seconds",
        "phase",
        "rss_bytes",
        "heap_used_bytes",
        "heap_committed_bytes",
        "heap_max_bytes",
        "metaspace_used_bytes",
        "direct_used_bytes",
        "live_threads",
        "loaded_classes",
        "metric_errors",
    ]
    with (output / "samples.csv").open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(target, fieldnames=fields)
        writer.writeheader()
        for sample in samples:
            row = dict(sample)
            row["metric_errors"] = ";".join(sample["metric_errors"])
            writer.writerow(row)

    payload = dict(summary)
    payload["threshold_failures"] = failures
    (output / "summary.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    rabbit = "habilitado" if summary["rabbit_enabled"] else "desabilitado"
    lines = [
        f"# Benchmark de memória — {summary['label']} / RabbitMQ {rabbit}",
        "",
        f"- Requisições: **{summary['requests']['total']}**",
        f"- Sucesso: **{summary['requests']['success_percent']:.2f}%**",
        f"- Latência p95: **{summary['requests']['latency_ms_p95']:.2f} ms**",
        f"- Amostras: **{summary['sample_count']}**",
        "",
        "| Métrica | Máximo | p95 | Inclinação |",
        "|---|---:|---:|---:|",
    ]
    for key, title in (
        ("rss", "RSS"),
        ("heap_used", "Heap usado"),
        ("metaspace_used", "Metaspace"),
        ("direct_used", "Memória direta"),
    ):
        item = summary["memory"][key]
        lines.append(
            f"| {title} | {item['max_mib']:.2f} MiB | {item['p95_mib']:.2f} MiB | "
            f"{item['slope_mib_per_min']:.2f} MiB/min |"
        )
    lines.extend(
        [
            f"| Threads vivas | {summary['threads']['max']:.0f} | {summary['threads']['p95']:.0f} | — |",
            "",
            "## Avaliação",
            "",
        ]
    )
    if failures:
        lines.extend(f"- FALHA: {failure}" for failure in failures)
    else:
        lines.append("- Limites atendidos.")
    (output / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    output = pathlib.Path(args.output_dir)
    thresholds = read_thresholds(pathlib.Path(args.thresholds))
    secret = os.environ.get("SECURITY_JWT_SECRET", "cloudport-memory-jwt-secret-with-at-least-32-bytes")
    token = jwt(secret)
    stop = threading.Event()
    stats = RequestStats()
    samples: list[dict[str, Any]] = []
    total_seconds = args.warmup_seconds + args.duration_seconds
    started = time.monotonic()

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
        workers = [executor.submit(load_worker, stop, args, token, stats) for _ in range(args.concurrency)]
        next_sample = started
        try:
            while True:
                now = time.monotonic()
                elapsed = now - started
                if elapsed >= total_seconds:
                    break
                if now >= next_sample:
                    phase = "warmup" if elapsed < args.warmup_seconds else "measurement"
                    samples.append(collect_sample(args, token, elapsed, phase))
                    next_sample += args.sample_interval
                time.sleep(min(0.2, max(0.0, next_sample - time.monotonic())))
        finally:
            stop.set()
            for worker in workers:
                worker.result(timeout=max(10.0, args.timeout_seconds * 2))

    summary = summarize(args, samples, stats.snapshot())
    failures = evaluate(summary, thresholds) if args.enforce_thresholds else []
    write_outputs(output, samples, summary, failures)
    print(json.dumps({"summary": summary, "failures": failures}, ensure_ascii=False))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
