# Plano MVP do Terminal Operating System (TOS)

## Visão Geral
Este documento consolida os requisitos prioritários para o MVP do TOS, organizando-os em frentes de produto, tecnologia e operação. O objetivo é garantir que o desenvolvimento inicie com foco nas maiores dores dos clientes de terminais portuários e gere impacto imediato em eficiência operacional, visibilidade e qualidade de serviço.

## Objetivos Estratégicos
1. **Maximizar a utilização de ativos críticos**: reduzir tempos de espera de navios e caminhões, elevando a produtividade do cais e do pátio.
2. **Digitalizar e automatizar decisões operacionais**: substituir planilhas e sistemas legados por ferramentas inteligentes e colaborativas.
3. **Empoderar clientes e parceiros**: oferecer transparência de ponta a ponta e processos de autoatendimento confiáveis.
4. **Construir base escalável**: arquiteturas modulares e APIs seguras para permitir evolução rápida e integrações futuras.

## Backlog Prioritário

### 1. Front-end React

| Epic | Descrição | Entregas Principais | Métrica de Sucesso |
| --- | --- | --- | --- |
| **F1. Painel de Berth Planning** | Visualização interativa do cais em linha do tempo com drag-and-drop de navios e alocação de guindastes. | - Canvas temporal com slots de berços.<br>- Regras de conflito e validação visual.<br>- Integração com disponibilidade de STS/Portainers.<br>- Histórico de versões e "what-if". | Tempo de replanejamento reduzido de minutos para segundos. |
| **F2. Mapa 2D do Pátio** | Mapa em tempo real exibindo contêineres por status e equipamentos móveis. | - Grid do pátio com camadas de status.<br>- Atualização em tempo real via WebSockets.<br>- Filtro por booking, destino, tipo de carga.<br>- Exibição de RTGs e Reach Stackers. | +15% moves per hour por equipamento. |

### 2. Back-end Java

| Epic | Descrição | Entregas Principais | Métrica de Sucesso |
| --- | --- | --- | --- |
| **B1. Agendamento de Time Slots** | API segura para agendar janelas de caminhões com validação automática de dados de carga. | - Autenticação e autorização via OAuth2.<br>- Validação de booking, contêiner e documentos fiscais.<br>- Motor de regras de capacidade por janela.<br>- Monitoramento de SLA e no-show. | 90% dos agendamentos validados sem intervenção humana. |
| **B2. Otimização de Alocação de Contêineres** | Microsserviço de recomendação de posição ideal no pátio visando minimizar rehandling. | - Modelo heurístico ou baseado em score.<br>- API de sugestão e feedback de execução.<br>- Telemetria sobre ganhos vs. baseline.<br>- Suporte a múltiplos objetivos (navio, trem, gate). | -20% rehandling. |

### 3. Produto & Negócio

| Epic | Descrição | Entregas Principais | Métrica de Sucesso |
| --- | --- | --- | --- |
| **P1. Portal Self-Service** | Portal para clientes rastrearem carga, visualizar cobranças e agendar retirada. | - Dashboard de status de contêiner.<br>- Notificações proativas (email/SMS).<br>- Histórico de cobranças e pagamentos.<br>- Workflows de agendamento de retirada. | -40% contatos sobre status de contêiner. |
| **P2. Faturamento Automatizado** | Módulo que gera cobranças baseadas em eventos operacionais. | - Motor de tarifação configurável.<br>- Consolidação automática de eventos.<br>- Integração com ERP/contas a receber.<br>- Relatórios de auditoria. | -95% erros de faturamento e -50% no DSO. |

## Sequenciamento e Marcos
1. **Sprint 0 (2 semanas)**
   - Definir design system React (Storybook).
   - Mapear integrações existentes (TOS legado, ERP, RFID).
   - Configurar pipelines CI/CD e observabilidade.

2. **Fase 1 (6-8 semanas)**
   - Entregar MVP do Berth Planning com dados mockados.
   - Disponibilizar API de agendamento com validações básicas.
   - Prototipar portal self-service com rastreamento.

3. **Fase 2 (8-10 semanas)**
   - Integrar dados em tempo real para mapa 2D.
   - Evoluir API de agendamento com regras avançadas e dashboards.
   - Lançar piloto de faturamento automatizado com subset de eventos.

4. **Fase 3 (10-12 semanas)**
   - Iniciar rollout do microsserviço de otimização com feedback loop.
   - Ampliar portal self-service com pagamentos online e suporte.
   - Ajustar KPIs com base em telemetria e feedback dos usuários.

## Requisitos Técnicos Transversais
- **Arquitetura de Eventos**: adotar Kafka ou similar para publicar eventos operacionais (movimentação, gate, faturamento).
- **Observabilidade**: métricas (Prometheus), logs estruturados e tracing distribuído (OpenTelemetry).
- **Segurança**: OAuth2 + OpenID Connect, criptografia em trânsito e repouso, RBAC baseado em perfis operacionais.
- **Dados e Analytics**: data lake para armazenar histórico, dashboards em BI (Looker, Power BI) para KPIs.

## Próximos Passos Recomendados
1. Validar com stakeholders os fluxos detalhados do Berth Planning e mapa do pátio.
2. Definir modelo de dados mestre para navios, contêineres, equipamentos e eventos.
3. Alinhar integrações com parceiros (armadores, transportadoras) para APIs abertas.
4. Estabelecer governança de backlog e cadência de discovery contínuo com operações.

---
Este roadmap prioriza ganhos operacionais imediatos enquanto prepara a base para funcionalidades avançadas que diferenciam o terminal no médio prazo.
