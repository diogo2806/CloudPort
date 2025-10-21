# Procedimentos operacionais do servico-gate

Este documento define procedimentos padrão (SOP) para operação contínua do **servico-gate** e ações de contingência quando houver indisponibilidade parcial ou total do módulo.

## Objetivos de disponibilidade

- **SLA**: 99,5% mensal para APIs síncronas (`/gate/entrada`, `/gate/saida`) e webhooks (`/webhooks/gate/**`).
- **RTO**: até 30 minutos para restabelecimento após falhas.
- **RPO**: máximo de 5 minutos de perda de eventos, compensados pelo reenvio do middleware local.

## Rotina operacional diária

1. **Monitoramento**
   - Verificar dashboards de métricas (Prometheus/Grafana) para latência das APIs de gate, taxa de sucesso (`gate.middleware.eventos.processados`) e erros por tipo de evento.
   - Acompanhar alertas do PagerDuty ou canal definido via webhook.
2. **Verificação do middleware local**
   - Confirmar que o middleware está autenticando com sucesso (métricas `http.server.requests` com `uri=/webhooks/gate/*`).
   - Revisar logs de integração em busca de falhas de autenticação ou payloads inválidos.
3. **Processamento de OCR**
   - Garantir que a fila `cloudport.gate.ocr.solicitacao-queue` esteja sem backlog persistente (verificar via RabbitMQ apenas para OCR).
4. **Backups**
   - Validar execução do backup incremental diário da base `servico_gate` e do bucket `cloudport-documents`.
5. **Relatórios**
   - Consolidar ocorrências no relatório diário de gate (Google Sheets/PowerBI).

## Contingência operacional

| Cenário | Indicadores | Ação imediata | Comunicação |
|---------|-------------|---------------|-------------|
| Falha geral da API (HTTP 5xx persistente) | Alertas do APIM/Grafana | Ativar modo contingência manual, registrar eventos em planilha padrão e abrir incidente SEV-2 | Notificar NOC e operações via Slack #gate-ops |
| Falha de autenticação do middleware | Métricas `http.server.requests` com status 401/403 | Validar credenciais do cliente `gate.middleware`, renovar token e solicitar reenvio | Comunicar equipe de TI do terminal |
| Latência > 3s nos webhooks | Métricas de APM | Acionar time de SRE para scale-out manual (`kubectl scale deploy/servico-gate --replicas=4`) | Comunicar operador líder |
| Indisponibilidade do storage | Alertas MinIO/S3 | Ativar storage secundário e usar upload offline com tablets | Avisar área de documentação |

### Modo contingência manual

1. Registrar cada gate (entrada e saída) em planilha local com data/hora, motorista, placa e protocolo físico.
2. Capturar fotos em câmera offline e sincronizar posteriormente para o bucket `cloudport-documents`.
3. Após restabelecimento, utilizar o middleware local ou `tools/api/servico-gate.http` para reenviar os eventos pendentes.
4. Registrar reconciliação no painel de auditoria (`/transparencia/dashboard`).

### Reenvio de eventos do middleware

1. Solicitar ao time local que exporte os eventos pendentes no middleware do terminal.
2. Reprocessar cada evento com `POST /webhooks/gate/entrada` ou `POST /webhooks/gate/saida`, preservando o `timestamp` original.
3. Monitorar métricas `gate.middleware.eventos.processados` para garantir que todos os eventos foram aceitos.

## Checklist pós-incidente

1. Atualizar o incidente no ITSM com causa raiz e plano de ação.
2. Criar tarefa de follow-up para ajustes de configuração (ex.: tuning de timeouts ou circuit-breakers).
3. Registrar aprendizado na wiki de operações.
4. Revisar regras de alerta e thresholds.
