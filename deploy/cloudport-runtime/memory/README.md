# Baseline e regressão de memória do cloudport-runtime

Este diretório contém um cenário reproduzível para medir o monólito modular sem remover módulos de negócio. A execução compara o commit anterior ao PR #649 com a revisão atual, sempre nos modos `RABBITMQ_ENABLED=false` e `RABBITMQ_ENABLED=true`.

## O que é medido

- RSS do processo Java obtido em `/proc/<pid>/status` pelo PID do container;
- heap usado, comprometido e máximo via Spring Boot Actuator;
- metaspace via `jvm.memory.used` com a tag `id:Metaspace`;
- memória direta via `jvm.buffer.memory.used` com a tag `id:direct`;
- threads vivas e classes carregadas;
- inclinação de RSS e heap durante a janela de medição;
- total, sucesso e latência das requisições de carga.

A carga percorre endpoints de infraestrutura e negócio: health, estado do corte operacional, status do pátio, OpenAPI e consulta de navios. O mesmo número de trabalhadores, aquecimento, duração e limite de container é usado em todos os cenários.

## Execução local

```bash
CLOUDPORT_MEMORY_WARMUP_SECONDS=15 \
CLOUDPORT_MEMORY_DURATION_SECONDS=45 \
CLOUDPORT_MEMORY_CONCURRENCY=12 \
bash deploy/cloudport-runtime/memory/benchmark.sh
```

As evidências são gravadas por padrão em `/tmp/cloudport-memory-evidence`:

- `report.md`: comparação consolidada antes/depois;
- `comparison.json`: comparação legível por automação;
- `static-risk-audit.md`: auditoria heurística de métricas, caches e JPA;
- `<revisão>-rabbit-<modo>/samples.csv`: série temporal completa;
- `<revisão>-rabbit-<modo>/summary.json`: resumo estruturado;
- `<revisão>-rabbit-<modo>/summary.md`: resumo humano;
- logs do benchmark, aplicação e Docker Compose.

## Limites versionados

Os limites ficam em `thresholds.env`. A revisão atual falha quando ultrapassa limites absolutos, apresenta crescimento sustentado ou executa menos de 99% das requisições com sucesso. A comparação também falha quando a revisão atual excede 115% do baseline histórico em RSS, heap, metaspace, memória direta ou threads.

Alterações nesses valores devem ser acompanhadas pelo artefato de uma execução com carga equivalente. Não aumente limites apenas para tornar o CI verde.

## Baseline histórico

O padrão é o commit `8f3d7643b1615975940e978d24c8772c63d2366c`, imediatamente anterior ao PR #649. Outro commit pode ser informado:

```bash
CLOUDPORT_MEMORY_BASELINE_REF=<commit> \
bash deploy/cloudport-runtime/memory/benchmark.sh
```

O workflow `Regressão de memória do runtime` também aceita esse valor por execução manual.

## Valores iniciais recomendados

Os valores abaixo são pontos de partida. A decisão final deve considerar pico real, latência, quantidade de conexões e filas acumuladas.

| Perfil | Limite do container | MaxRAM | Metaspace | Direta | Hikari | Tomcat | Async | OCR |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Pequeno | 1536 MiB | 65% | 384 MiB | 256 MiB | 8 | 50 | 2–4 | 1–2 |
| Médio | 3072 MiB | 65% | 512 MiB | 384 MiB | 16 | 100 | 4–8 | 2–4 |
| Grande | 6144 MiB | 70% | 768 MiB | 512 MiB | 30 | 200 | 8–16 | 4–8 |

Aumentar Tomcat, Hikari e executores simultaneamente eleva memória nativa, contenção e pressão no banco. Cada ajuste deve ser reexecutado nos dois modos de RabbitMQ.

## Auditoria estática

`audit_sources.py` lista padrões que merecem investigação:

- tags de métricas que podem receber identificadores únicos;
- caches e mapas locais sem tamanho ou expiração evidentes;
- `FetchType.EAGER`, `JOIN FETCH` e associações `ManyToOne`/`OneToOne` sem `LAZY` explícito.

Os achados são heurísticos e não substituem heap dump, Java Flight Recorder ou análise do plano SQL. Eles servem para direcionar a investigação quando a série temporal indicar crescimento.
