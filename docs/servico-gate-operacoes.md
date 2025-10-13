# Procedimentos operacionais do servico-gate

Este documento define procedimentos padrão (SOP) para operação contínua do **servico-gate** e ações de contingência quando houver indisponibilidade parcial ou total do módulo.

## Objetivos de disponibilidade

- **SLA**: 99,5% mensal para APIs síncronas (`/gate/in/**`, `/gate/out/**`).
- **RTO**: até 30 minutos para restabelecimento após falhas.
- **RPO**: máximo de 5 minutos de perda de eventos através de compensação via DLQ.

## Rotina operacional diária

1. **Monitoramento**
   - Verificar dashboards de métricas (Prometheus/Grafana) para consumo de CPU, backlog das filas `gate.in.process` e `gate.out.process` e latência de respostas.
   - Acompanhar alertas do PagerDuty ou canal definido via webhook.
2. **Verificação de filas**
   - Executar `rabbitmqadmin list queues name messages_ready messages_unacknowledged`.
   - Se `messages_unacknowledged > 100`, avaliar necessidade de escala manual.
3. **Backups**
   - Validar execução do backup incremental diário da base `servico_gate` e do bucket `cloudport-documents`.
4. **Relatórios**
   - Consolidar ocorrências no relatório diário de gate (Google Sheets/PowerBI).

## Contingência operacional

| Cenário | Indicadores | Ação imediata | Comunicação |
|---------|-------------|---------------|-------------|
| Falha geral da API (HTTP 5xx persistente) | Alertas do APIM/Grafana | Ativar modo contingência manual, registrar check-ins via planilha padrão e abrir incidente SEV-2 | Notificar NOC e operações via Slack #gate-ops |
| Degradação por latência > 3s | Métricas de APM | Acionar time de SRE para scale-out manual (kubectl scale deploy/servico-gate --replicas=4) | Comunicar operador líder |
| Fila `gate.in.dead-letter` crescente | RabbitMQ console | Executar script de reprocessamento (ver abaixo) após investigar causa | Registrar incidente menor |
| Indisponibilidade do storage | Alertas MinIO/S3 | Ativar storage secundário e usar upload offline com tablets | Avisar área de documentação |

### Modo contingência manual

1. Registrar cada gate-in/out em planilha local com data/hora, motorista, placa e protocolo físico.
2. Capturar fotos em câmera offline e sincronizar posteriormente para o bucket `cloudport-documents`.
3. Após restabelecimento, utilizar script `scripts/sync-contingencia.sh` (a ser desenvolvido) para inserir registros pendentes via API.
4. Revisar fila `gate.contingencia.replay` e garantir que todos os eventos sejam reconciliados.

### Reprocessamento de DLQ

1. Conectar-se ao RabbitMQ: `kubectl port-forward svc/rabbitmq 15672:15672`.
2. Executar script:

```bash
rabbitmqadmin get queue=gate.in.dead-letter requeue=true
rabbitmqadmin get queue=gate.out.dead-letter requeue=true
```

3. Monitorar métricas de consumo até que a fila volte a zero.

## Checklist pós-incidente

1. Atualizar o incidente no ITSM com causa raiz e plano de ação.
2. Criar tarefa de follow-up para ajustes de configuração (ex.: tunning de timeouts ou circuit-breakers).
3. Registrar aprendizado na wiki de operações.
4. Revisar regras de alerta e thresholds.
