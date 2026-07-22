#!/usr/bin/env python3
"""Gera uma auditoria informativa de riscos comuns de retenção de memória."""

from __future__ import annotations

import argparse
import pathlib
import re
from dataclasses import dataclass

SUSPICIOUS_CARDINALITY = re.compile(
    r"(?i)(usuario|user|container|conteiner|navio|booking|referencia|placa|viagem|visit|id)"
)
METRIC_CALL = re.compile(r"(?i)(\.tag\s*\(|\.tags\s*\(|Tags\.of\s*\(|MeterRegistry)")
JPA_RELATION = re.compile(r"@(ManyToOne|OneToOne)\s*(\([^)]*\))?", re.DOTALL)


@dataclass(frozen=True)
class Finding:
    category: str
    path: str
    line: int
    detail: str


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def compact(value: str, limit: int = 180) -> str:
    normalized = " ".join(value.strip().split())
    return normalized if len(normalized) <= limit else normalized[: limit - 3] + "..."


def audit_file(root: pathlib.Path, path: pathlib.Path) -> list[Finding]:
    text = path.read_text(encoding="utf-8", errors="replace")
    relative = str(path.relative_to(root))
    findings: list[Finding] = []

    for index, line in enumerate(text.splitlines(), start=1):
        if METRIC_CALL.search(line) and SUSPICIOUS_CARDINALITY.search(line):
            findings.append(
                Finding(
                    "Métrica com cardinalidade potencial",
                    relative,
                    index,
                    compact(line),
                )
            )
        if "FetchType.EAGER" in line:
            findings.append(Finding("Relacionamento JPA EAGER explícito", relative, index, compact(line)))
        if re.search(r"(?i)\bjoin\s+fetch\b", line):
            findings.append(Finding("Consulta com JOIN FETCH", relative, index, compact(line)))

    for match in JPA_RELATION.finditer(text):
        annotation = match.group(0)
        if "FetchType.LAZY" not in annotation:
            findings.append(
                Finding(
                    "Relacionamento JPA com padrão EAGER",
                    relative,
                    line_number(text, match.start()),
                    compact(annotation),
                )
            )

    uses_local_cache = any(
        marker in text
        for marker in (
            "Caffeine.newBuilder",
            "CacheBuilder.newBuilder",
            "ConcurrentHashMap",
            "Collections.synchronizedMap",
        )
    )
    has_bound = any(
        marker in text
        for marker in (
            "maximumSize(",
            "maximumWeight(",
            "expireAfter",
            "removeEldestEntry",
            "cloudport.tos.cache.max-size",
        )
    )
    if uses_local_cache and not has_bound:
        first_offset = min(
            (text.find(marker) for marker in ("Caffeine.newBuilder", "CacheBuilder.newBuilder", "ConcurrentHashMap", "Collections.synchronizedMap") if marker in text),
            default=0,
        )
        findings.append(
            Finding(
                "Cache ou mapa local sem limite evidente",
                relative,
                line_number(text, first_offset),
                "Revisar expiração, tamanho máximo e remoção de entradas.",
            )
        )

    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    root = pathlib.Path(args.root).resolve()
    findings: list[Finding] = []
    for path in sorted((root / "backend").rglob("*")):
        if path.suffix not in {".java", ".properties", ".yml", ".yaml"}:
            continue
        if any(part in {"target", "build"} for part in path.parts):
            continue
        findings.extend(audit_file(root, path))

    grouped: dict[str, list[Finding]] = {}
    for finding in findings:
        grouped.setdefault(finding.category, []).append(finding)

    lines = [
        "# Auditoria estática de riscos de memória",
        "",
        "A auditoria é heurística: cada ocorrência precisa de confirmação por perfil de heap, métricas e plano de execução SQL.",
        "",
        f"Total de ocorrências: **{len(findings)}**.",
        "",
    ]
    if not findings:
        lines.append("Nenhum padrão conhecido foi encontrado.")
    for category in sorted(grouped):
        items = grouped[category]
        lines.extend((f"## {category}", "", f"Ocorrências: **{len(items)}**.", ""))
        for item in items[:100]:
            escaped = item.detail.replace("`", "'")
            lines.append(f"- `{item.path}:{item.line}` — `{escaped}`")
        if len(items) > 100:
            lines.append(f"- Mais {len(items) - 100} ocorrências omitidas.")
        lines.append("")

    pathlib.Path(args.output).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"audit_findings={len(findings)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
