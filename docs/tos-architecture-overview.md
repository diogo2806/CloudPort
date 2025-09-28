# Visão de Arquitetura do TOS

## Camadas Principais
1. **Experiência do Usuário (React)**
   - Aplicação SPA com micro-frontends opcionais.
   - State management com Zustand ou Redux Toolkit.
   - Comunicação em tempo real via WebSockets/SSE.
   - Design system responsivo (componentes React + Tailwind/Chakra).

2. **APIs e Microsserviços (Java + Spring Boot)**
   - Microsserviços independentes por domínio (Berth, Yard, Gate, Billing).
   - Contratos REST com OpenAPI e versionamento.
   - Integração assíncrona via Kafka (event sourcing / CQRS light).
   - Segurança: Keycloak como provedor OIDC, API Gateway com rate limiting.

3. **Data & Analytics**
   - Data Lake (S3/GCS) alimentado por streams.
   - Modelos analíticos em BigQuery/Snowflake.
   - Ferramentas de BI para KPIs (berth utilization, gate SLAs, dwell time).

4. **Infraestrutura & DevOps**
   - Kubernetes (EKS/AKS/GKE) com Helm Charts.
   - Pipelines CI/CD (GitHub Actions) com testes, SAST e deploy automatizado.
   - Observabilidade: Grafana, Prometheus, Loki, Tempo.
   - Feature flags (LaunchDarkly ou OpenFeature).

## Contexto de Domínio
```
[Clientes] --(Self-Service Portal)--> [API Gateway] --> [Microsserviços]
                                                      |--> Berth Planning Service
                                                      |--> Yard Visibility Service
                                                      |--> Gate Scheduling Service
                                                      |--> Billing Service
                                                      |--> Optimization Service

[Microsserviços] --(Eventos)--> [Kafka] --(ETL)--> [Data Lake / Warehouse]
[Microsserviços] --(Comandos)--> [PostgreSQL / Redis]
```

## Integrações Externas
- **Armadores**: consumo de dados de ETA/ETD, manifests e requisições de guindastes.
- **Transportadoras**: APIs abertas para agendamento e notificação de gate-in/out.
- **Sistemas Legados**: importação de dados do TOS existente via ETL ou replicação.
- **Autoridades Portuárias**: relatórios regulatórios automatizados.

## Segurança e Compliance
- RBAC com perfis (Planejador, Operador Pátio, Financeiro, Cliente).
- Auditoria de ações críticas (edição de janela, confirmação de faturamento).
- LGPD: mascaramento de dados pessoais, logs anonimizados.

## Roadmap Técnico
1. Definir modelo de dados comum (DDD + Event Storming).
2. Configurar plataforma de eventos e gateway de APIs.
3. Estabelecer contratos de integração com fontes externas.
4. Implantar observabilidade mínima (métricas + logs estruturados).
5. Evoluir com machine learning para otimização de pátio.

---
Este documento deve ser atualizado a cada fase de evolução do TOS para refletir decisões arquiteturais e integrações críticas.
