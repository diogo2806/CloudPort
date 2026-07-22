#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
MEMORY_DIR="$ROOT_DIR/deploy/cloudport-runtime/memory"
COMPOSE_FILE="$ROOT_DIR/deploy/cloudport-runtime/docker-compose.memory.yml"
THRESHOLDS_FILE="$MEMORY_DIR/thresholds.env"
OUTPUT_DIR="${CLOUDPORT_MEMORY_OUTPUT_DIR:-/tmp/cloudport-memory-evidence}"
BASELINE_REF="${CLOUDPORT_MEMORY_BASELINE_REF:-8f3d7643b1615975940e978d24c8772c63d2366c}"
CURRENT_REF="$(git -C "$ROOT_DIR" rev-parse HEAD)"
WARMUP_SECONDS="${CLOUDPORT_MEMORY_WARMUP_SECONDS:-15}"
DURATION_SECONDS="${CLOUDPORT_MEMORY_DURATION_SECONDS:-45}"
CONCURRENCY="${CLOUDPORT_MEMORY_CONCURRENCY:-12}"
SAMPLE_INTERVAL="${CLOUDPORT_MEMORY_SAMPLE_INTERVAL:-2}"
BASE_PORT="${CLOUDPORT_MEMORY_BASE_PORT:-18082}"
RUN_ID="${GITHUB_RUN_ID:-local}-$$"
BASELINE_WORKTREE="/tmp/cloudport-memory-baseline-$RUN_ID"
CURRENT_IMAGE="cloudport-runtime-memory-current:$RUN_ID"
BASELINE_IMAGE="cloudport-runtime-memory-baseline:$RUN_ID"
ACTIVE_PROJECT=""

export DB_NAME="${DB_NAME:-cloudport_memory}"
export DB_USER="${DB_USER:-cloudport}"
export DB_PASS="${DB_PASS:-cloudport-memory-postgres}"
export RABBITMQ_USERNAME="${RABBITMQ_USERNAME:-cloudport}"
export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-cloudport-memory-rabbit}"
export SECURITY_JWT_SECRET="${SECURITY_JWT_SECRET:-cloudport-memory-jwt-secret-with-at-least-32-bytes}"

set -a
# shellcheck disable=SC1090
. "$THRESHOLDS_FILE"
set +a

log() {
  printf '[memory-benchmark] %s\n' "$*"
}

compose_for() {
  local project=$1
  shift
  docker compose -p "$project" -f "$COMPOSE_FILE" "$@"
}

cleanup() {
  local exit_code=$?
  if [[ -n "$ACTIVE_PROJECT" ]]; then
    compose_for "$ACTIVE_PROJECT" down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  git -C "$ROOT_DIR" worktree remove --force "$BASELINE_WORKTREE" >/dev/null 2>&1 || true
  docker image rm "$CURRENT_IMAGE" "$BASELINE_IMAGE" >/dev/null 2>&1 || true
  exit "$exit_code"
}
trap cleanup EXIT INT TERM

wait_http() {
  local url=$1
  local attempts=${2:-120}
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS --max-time 4 "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

build_images() {
  log "construindo imagem atual $CURRENT_REF"
  docker build -f "$ROOT_DIR/backend/cloudport-runtime/Dockerfile" -t "$CURRENT_IMAGE" "$ROOT_DIR"

  log "preparando baseline histórico $BASELINE_REF"
  rm -rf "$BASELINE_WORKTREE"
  git -C "$ROOT_DIR" worktree add --detach "$BASELINE_WORKTREE" "$BASELINE_REF"
  docker build \
    -f "$BASELINE_WORKTREE/backend/cloudport-runtime/Dockerfile" \
    -t "$BASELINE_IMAGE" \
    "$BASELINE_WORKTREE"
}

run_case() {
  local label=$1
  local image=$2
  local rabbit_enabled=$3
  local port=$4
  local enforce=$5
  local project="cloudport-memory-${label}-${rabbit_enabled}-${RUN_ID}"
  local case_dir="$OUTPUT_DIR/${label}-rabbit-${rabbit_enabled}"
  local status=0

  mkdir -p "$case_dir"
  ACTIVE_PROJECT="$project"
  export CLOUDPORT_MEMORY_IMAGE="$image"
  export CLOUDPORT_MEMORY_PORT="$port"
  export CLOUDPORT_MEMORY_LABEL="$label-rabbit-$rabbit_enabled"
  export CLOUDPORT_MEMORY_REVISION="$([[ "$label" == "baseline" ]] && printf '%s' "$BASELINE_REF" || printf '%s' "$CURRENT_REF")"
  export RABBITMQ_ENABLED="$rabbit_enabled"

  compose_for "$project" down -v --remove-orphans >/dev/null 2>&1 || true
  log "iniciando $label com RabbitMQ=$rabbit_enabled"
  if ! compose_for "$project" up -d; then
    status=1
  elif ! wait_http "http://localhost:${port}/actuator/health/readiness" 150; then
    log "timeout na inicialização de $label com RabbitMQ=$rabbit_enabled"
    status=1
  else
    local container
    container="$(compose_for "$project" ps -q cloudport-runtime)"
    local enforce_args=()
    if [[ "$enforce" == "true" ]]; then
      enforce_args+=(--enforce-thresholds)
    fi
    set +e
    python3 "$MEMORY_DIR/benchmark.py" \
      --base-url "http://localhost:${port}" \
      --container "$container" \
      --label "$label" \
      --rabbit-enabled "$rabbit_enabled" \
      --output-dir "$case_dir" \
      --thresholds "$THRESHOLDS_FILE" \
      --warmup-seconds "$WARMUP_SECONDS" \
      --duration-seconds "$DURATION_SECONDS" \
      --concurrency "$CONCURRENCY" \
      --sample-interval "$SAMPLE_INTERVAL" \
      "${enforce_args[@]}" \
      > "$case_dir/benchmark.log" 2>&1
    benchmark_status=$?
    set -e
    cat "$case_dir/benchmark.log"
    if [[ $benchmark_status -ne 0 ]]; then
      status=1
    fi
  fi

  compose_for "$project" ps > "$case_dir/compose-ps.txt" 2>&1 || true
  compose_for "$project" logs --no-color --tail=500 > "$case_dir/compose.log" 2>&1 || true
  compose_for "$project" down -v --remove-orphans >/dev/null 2>&1 || true
  ACTIVE_PROJECT=""
  return "$status"
}

compare_results() {
  python3 - "$OUTPUT_DIR" "$BASELINE_REF" "$CURRENT_REF" "$MAX_CURRENT_VS_BASELINE_PERCENT" <<'PY'
from __future__ import annotations

import json
import pathlib
import sys

folder = pathlib.Path(sys.argv[1])
baseline_ref = sys.argv[2]
current_ref = sys.argv[3]
max_percent = float(sys.argv[4])
failures = []
rows = []

metrics = (
    ("RSS", ("memory", "rss", "max_mib"), "MiB"),
    ("Heap usado", ("memory", "heap_used", "max_mib"), "MiB"),
    ("Metaspace", ("memory", "metaspace_used", "max_mib"), "MiB"),
    ("Memória direta", ("memory", "direct_used", "max_mib"), "MiB"),
    ("Threads", ("threads", "max"), ""),
)

def load(label, rabbit):
    path = folder / f"{label}-rabbit-{rabbit}" / "summary.json"
    if not path.exists():
        failures.append(f"resultado ausente: {path}")
        return None
    return json.loads(path.read_text(encoding="utf-8"))

def value(payload, path):
    current = payload
    for key in path:
        current = current[key]
    return float(current)

for rabbit in ("false", "true"):
    baseline = load("baseline", rabbit)
    current = load("current", rabbit)
    if baseline is None or current is None:
        continue
    for name, path, unit in metrics:
        before = value(baseline, path)
        after = value(current, path)
        percent = after * 100.0 / before if before > 0 else 0.0
        rows.append((rabbit, name, before, after, percent, unit))
        if before > 0 and percent > max_percent:
            failures.append(
                f"RabbitMQ={rabbit} {name}: revisão atual em {percent:.2f}% do baseline, limite {max_percent:.2f}%"
            )

lines = [
    "# Relatório comparativo de memória do cloudport-runtime",
    "",
    f"- Baseline anterior ao PR #649: `{baseline_ref}`",
    f"- Revisão avaliada: `{current_ref}`",
    f"- Limite relativo: **{max_percent:.2f}%** do baseline histórico",
    "",
    "| RabbitMQ | Métrica | Antes do #649 | Revisão atual | Atual / antes |",
    "|---|---|---:|---:|---:|",
]
for rabbit, name, before, after, percent, unit in rows:
    suffix = f" {unit}" if unit else ""
    lines.append(
        f"| {rabbit} | {name} | {before:.2f}{suffix} | {after:.2f}{suffix} | {percent:.2f}% |"
    )
lines.extend(("", "## Resultado", ""))
if failures:
    lines.extend(f"- FALHA: {failure}" for failure in failures)
else:
    lines.append("- Comparação aprovada.")
lines.extend(
    (
        "",
        "## Evidências",
        "",
        "Cada cenário contém `samples.csv`, `summary.json`, `summary.md`, logs do benchmark e logs do Compose.",
        "A auditoria estática está em `static-risk-audit.md`.",
    )
)
(folder / "report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
(folder / "comparison.json").write_text(
    json.dumps(
        {
            "baseline_ref": baseline_ref,
            "current_ref": current_ref,
            "max_current_vs_baseline_percent": max_percent,
            "rows": [
                {
                    "rabbit_enabled": rabbit == "true",
                    "metric": name,
                    "baseline": before,
                    "current": after,
                    "current_vs_baseline_percent": percent,
                    "unit": unit,
                }
                for rabbit, name, before, after, percent, unit in rows
            ],
            "failures": failures,
        },
        ensure_ascii=False,
        indent=2,
    ),
    encoding="utf-8",
)
print("\n".join(lines))
raise SystemExit(1 if failures else 0)
PY
}

main() {
  command -v docker >/dev/null
  command -v git >/dev/null
  command -v python3 >/dev/null
  mkdir -p "$OUTPUT_DIR"
  rm -rf "$OUTPUT_DIR"/*

  python3 "$MEMORY_DIR/audit_sources.py" \
    --root "$ROOT_DIR" \
    --output "$OUTPUT_DIR/static-risk-audit.md"

  build_images

  local status=0
  run_case baseline "$BASELINE_IMAGE" false "$BASE_PORT" false || status=1
  run_case baseline "$BASELINE_IMAGE" true "$((BASE_PORT + 1))" false || status=1
  run_case current "$CURRENT_IMAGE" false "$((BASE_PORT + 2))" true || status=1
  run_case current "$CURRENT_IMAGE" true "$((BASE_PORT + 3))" true || status=1
  compare_results || status=1

  printf 'baseline_ref=%s\ncurrent_ref=%s\nstatus=%s\n' \
    "$BASELINE_REF" "$CURRENT_REF" "$status" > "$OUTPUT_DIR/status.txt"
  exit "$status"
}

main "$@"
