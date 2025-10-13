# Playbook de Incidentes do Gate

Este playbook descreve como operar o serviço **servico-gate** durante incidentes que afetam integrações externas (TOS, autenticação ou hardware) e como utilizar os novos mecanismos de observabilidade e contingência.

## 1. Monitoramento e Alertas

Os seguintes painéis e alertas devem ser configurados no Prometheus/Grafana (ou ferramenta equivalente):

- **Tempo de validação do gate (`gate_validacao_tempo_seconds`)**: histograma exportado via `/actuator/prometheus`. Configure alerta quando o `p95` exceder 3 segundos por mais de 5 minutos.
- **Consumo das filas de hardware (`gate_hardware_mensagens_processadas_total`)**: acompanhe por `fila` (entrada/saída) e alerte para quedas abruptas ou crescimento de falhas.
- **Eventos de degradação das integrações (`gate_integracoes_degradacao_total`)**: alerte imediatamente quando o contador crescer mais de 3 vezes em 15 minutos, indicando circuit breakers abrindo.
- **Eventos de contingência (`gate_contingencia_eventos_total`)**: monitore para garantir retorno ao modo normal após contingências.
- **Health checks**: utilize `/actuator/health/readiness` e `/actuator/health/liveness` nas verificações automáticas. O health indicator `tos` fica `DOWN` quando o circuit breaker do TOS abre.

## 2. Detecção

1. Acompanhe os dashboards mencionados e configure notificações para o time de operações.
2. Um incidente é considerado crítico quando:
   - O circuit breaker `tosApi` fica aberto (`/actuator/health/readiness` reporta `tos: DOWN`).
   - Há aumento de `gate_integracoes_degradacao_total` associado a `autenticacao`.
   - O tempo de validação ultrapassa o SLA (p95 > 3 segundos).

## 3. Contenção Imediata

1. **Ativar contingência do gate**: definir `GATE_CONTINGENCIA_ENABLED=true` (ou `cloudport.gate.contingencia.enabled=true`) e reiniciar o serviço caso necessário. As rotas `/gate/contingencia/agendar` e `/gate/contingencia/liberar` passarão a aceitar requisições.
2. Informar os operadores para seguir a orientação automática retornada pelas rotas (ex: registrar manualmente os atendimentos e manter evidências).
3. Registrar manualmente as validações junto ao TOS assim que possível, seguindo o protocolo sugerido (`protocolo` retornado pelo endpoint deve ser usado para rastreabilidade).

## 4. Investigação

1. Verificar logs estruturados (JSON) filtrando por `event=integracao.degradada` para identificar qual integração apresentou falha.
2. Consultar traces distribuídos no backend de observabilidade (OTLP -> collector) usando o `service.name=servico-gate` e o `trace_id` presente nos logs.
3. Para falhas de autenticação, revisar os logs `event=autenticacao.fallback` e validar acessos temporários com a equipe de segurança.

## 5. Recuperação

1. Após restabelecer a integração, monitorar o fechamento automático dos circuit breakers (`tos` no health e métrica de degradação estabilizando).
2. Desativar a contingência (definir `GATE_CONTINGENCIA_ENABLED=false`).
3. Confirmar que o volume de mensagens nas filas voltou ao normal e que os tempos de validação estão dentro do SLA.

## 6. Pós-incidente

1. Consolidar todas as requisições processadas em contingência utilizando o `protocolo` retornado pelos endpoints.
2. Atualizar o playbook com eventuais melhorias identificadas.
3. Registrar RCA destacando métricas afetadas, alertas acionados e ações corretivas.

## 7. Checklist Rápido

- [ ] Contingência ativada? (`/gate/contingencia/*` respondendo 202)
- [ ] Alertas registrados em Prometheus/Grafana?
- [ ] Health checks (`/actuator/health/*`) retornando `UP` após mitigação?
- [ ] Logs com `event=integracao.degradada` tratados?
- [ ] Traces correlacionados com `trace_id` nas requisições críticas?
- [ ] Documentação de aprendizado registrada?
