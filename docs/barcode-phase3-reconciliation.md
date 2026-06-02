# Fase 3: Reconciliação Periódica e Detecção de Desincronias

## Visão Geral

Sistema automático de reconciliação que roda **à noite** (02:00 por padrão) para detectar e alertar sobre desincronias entre o espelho digital (banco de dados) e o pátio físico (TOS).

## Tipos de Desincronias Detectadas

| Tipo | Descrição | Severidade | Ação |
|------|-----------|-----------|------|
| **BARCODE_NAO_CONFIRMADO** | Container aguardando confirmação >30min | 🟡 Médio | Revisar DMT |
| **ENTRADA_SEM_SAIDA_24H** | Container na entrada >24h sem saída | 🔴 Alto | Localizar container no pátio |
| **BARCODE_MISMATCH** | Barcode local ≠ barcode TOS | 🔴 Alto | Validar manualmente |
| **STATUS_INCONSISTENTE** | Liberado local, bloqueado no TOS | 🔴 Alto | Sincronizar com TOS |
| **TIMEOUT_NAO_RESOLVIDO** | Timeout nunca foi tratado | 🟡 Médio | Verificar DMT offline |
| **CONTAINER_PRESO** | Entrou mas nunca saiu do sistema | 🔴 Alto | Auditoria completa |
| **DISCREPANCIA_TEMPORAL** | Entrada > Saída (timestamps) | 🔴 Crítico | Bug ou manipulação |

## Arquitetura

```
ReconciliacaoBarcodeScheduler
├─ @Scheduled(cron: "0 0 2 * * *")  ← 02:00 toda madrugada
│
├─ ReconciliacaoBarcodeService
│  ├─ verificarTimeoutPendente()
│  │  └─ GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
│  │     └─ dataEntrada < now - 30min? → ALERTA
│  │
│  ├─ verificarEntradaPresa()
│  │  └─ GatePass.status = EM_PROCESSAMENTO
│  │     └─ dataSaida = NULL e tempo > 24h? → CRÍTICO
│  │
│  └─ verificarConsistenciaBarcode()
│     ├─ TosIntegrationService.obterStatusContainer()
│     ├─ Comparar barcode local vs TOS
│     └─ Comparar status local vs TOS
│
└─ ReconciliacaoBarcodeRepository.save()
   └─ ReconciliacaoBarcode (auditoria)
```

## Flow Completo

```
02:00 (Cronjob inicia)
  ↓
[1] GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
    └─ dataEntrada < now - 30min?
       └─ SIM: ReconciliacaoBarcode { tipo: BARCODE_NAO_CONFIRMADO }
  ↓
[2] GatePass.status = EM_PROCESSAMENTO
    └─ dataSaida = NULL e tempo > 24h?
       └─ SIM: ReconciliacaoBarcode { tipo: ENTRADA_SEM_SAIDA_24H }
  ↓
[3] GatePass.status = LIBERADO
    ├─ Barcode local ≠ TOS?
    │  └─ SIM: ReconciliacaoBarcode { tipo: BARCODE_MISMATCH }
    │
    └─ Status local ≠ status TOS?
       └─ SIM: ReconciliacaoBarcode { tipo: STATUS_INCONSISTENTE }
  ↓
[4] Enviar alertas para operadores
    ├─ Email / Slack / SMS (TODO)
    └─ Log auditoria
  ↓
[5] Dashboard mostra problemas
    ├─ ADMIN_PORTO pode visualizar
    └─ Resolver manualmente
```

## Endpoints REST

### 1. Executar Reconciliação Imediatamente

```http
POST /gate/reconciliacao/executar
Authorization: Bearer $JWT_TOKEN
```

**Resposta (200 OK):**
```json
[
  {
    "id": 1,
    "gatePassId": 123,
    "codigoGatePass": "GP-001-ABC",
    "tipoDesinconia": "BARCODE_NAO_CONFIRMADO",
    "descricao": "Container aguardando confirmação de barcode há 45 minutos",
    "statusLocal": "AGUARDANDO_CONFIRMACAO_BARCODE",
    "tempoPendenciaHoras": 0,
    "detectadoEm": "2026-06-02T02:00:00",
    "resolvidoEm": null,
    "alerta Enviado": false
  }
]
```

---

### 2. Listar Desincronias Não Resolvidas

```http
GET /gate/reconciliacao/nao-resolvidas
Authorization: Bearer $JWT_TOKEN
```

**Resposta:**
```json
[
  {
    "id": 1,
    "gatePassId": 123,
    "codigoGatePass": "GP-001-ABC",
    "tipoDesinconia": "BARCODE_NAO_CONFIRMADO",
    "descricao": "...",
    "detectadoEm": "2026-06-02T02:00:00",
    "resolvidoEm": null
  },
  {
    "id": 2,
    "gatePassId": 456,
    "codigoGatePass": "GP-002-XYZ",
    "tipoDesinconia": "ENTRADA_SEM_SAIDA_24H",
    "descricao": "Container na entrada há 25 horas sem registrar saída",
    "tempoPendenciaHoras": 25,
    "detectadoEm": "2026-06-02T02:00:00",
    "resolvidoEm": null
  }
]
```

---

### 3. Filtrar por Tipo de Desincronização

```http
GET /gate/reconciliacao/por-tipo?tipo=BARCODE_MISMATCH
Authorization: Bearer $JWT_TOKEN
```

**Tipos disponíveis:**
- CONTAINER_PRESO
- BARCODE_NAO_CONFIRMADO
- BARCODE_MISMATCH
- TIMEOUT_NAO_RESOLVIDO
- STATUS_INCONSISTENTE
- ENTRADA_SEM_SAIDA_24H
- DISCREPANCIA_TEMPORAL

**Resposta:** Lista filtrada

---

### 4. Resolver Desincronização

```http
PUT /gate/reconciliacao/1/resolver
Authorization: Bearer $JWT_TOKEN
Content-Type: application/json

{
  "resolucao": "Container retirado manualmente do pátio - operador confirmou saída"
}
```

**Resposta (204 No Content)**

---

## Configuration

### application.properties

```properties
# Cronjob de reconciliação (Expressão CRON)
gate.reconciliacao.cron=${GATE_RECONCILIACAO_CRON:0 0 2 * * *}

# Padrão: 02:00 toda madrugada
# Exemplos:
# - 0 0 2 * * *    = 02:00 (padrão)
# - 0 0 1 * * *    = 01:00
# - 0 */6 * * * *  = a cada 6 horas
# - 0 0 0 * * MON  = segundas 00:00
```

### .env

```bash
GATE_RECONCILIACAO_CRON=0 0 2 * * *
```

## Database

### Tabela reconciliacao_barcode

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | BIGINT | PK |
| `gate_pass_id` | BIGINT | FK para gate_pass |
| `tipo_desinconia` | VARCHAR(50) | Tipo de problema |
| `descricao` | VARCHAR(1000) | Descrição detalhada |
| `barcode_esperado` | VARCHAR(50) | Barcode do TOS |
| `barcode_recebido` | VARCHAR(50) | Barcode local |
| `status_tos` | VARCHAR(50) | Status no TOS |
| `status_local` | VARCHAR(50) | Status local |
| `tempo_pendencia_horas` | INTEGER | Horas sem resolução |
| `detectado_em` | TIMESTAMP | Quando foi detectado |
| `resolvido_em` | TIMESTAMP | Quando foi resolvido |
| `resolucao` | VARCHAR(500) | Como foi resolvido |
| `alerta_enviado` | BOOLEAN | Se alerta foi notificado |

### Índices

```sql
idx_reconciliacao_gate_pass      -- Buscar por gate pass
idx_reconciliacao_tipo           -- Filtrar por tipo
idx_reconciliacao_resolvido      -- Status de resolução
idx_reconciliacao_detectado      -- Histórico por data
idx_reconciliacao_alerta         -- Alertas não enviados
```

### View: reconciliacao_nao_resolvida

```sql
SELECT
  r.id,
  r.gate_pass_id,
  gp.codigo,
  r.tipo_desinconia,
  r.descricao,
  r.tempo_pendencia_horas,
  r.detectado_em,
  EXTRACT(HOUR FROM (NOW() - r.detectado_em)) as horas_sem_resolucao
FROM reconciliacao_barcode r
JOIN gate_pass gp ON r.gate_pass_id = gp.id
WHERE r.resolvido_em IS NULL
ORDER BY r.detectado_em DESC;
```

## Logging

### Reconciliação Iniciada
```
event=reconciliacao.iniciada timestamp=2026-06-02T02:00:00
```

### Problema Detectado
```
event=reconciliacao.barcode_pendente gatePassId=123 minutos=45 timestamp=2026-06-02T02:05:00

event=reconciliacao.container_preso gatePassId=456 horas=25 timestamp=2026-06-02T02:10:00

event=reconciliacao.barcode_mismatch gatePassId=789 esperado=CONT123 recebido=CONT456 timestamp=2026-06-02T02:15:00
```

### Alerta Enviado
```
event=reconciliacao.alerta.enviado id=1 tipo=BARCODE_NAO_CONFIRMADO gatePass=GP-001 timestamp=2026-06-02T02:20:00
```

### Desincronização Resolvida
```
event=reconciliacao.resolvida id=1 tipo=BARCODE_NAO_CONFIRMADO resolucao="Container retirado manualmente" timestamp=2026-06-02T10:30:00
```

## Casos de Uso

### Caso 1: Container Preso (DMT Offline)

```
T+00:00 - Operador gate registra entrada
          → GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
          → Solicita confirmação ao DMT

T+00:30 - DMT offline, não responde
          → Timer de 30s expira, timeout handler executa
          → GatePass.status = RETIDO
          → Container bloqueado

T+02:00 - Cronjob executa reconciliação
          → Detecta: timeout não resolvido há 1.5 horas
          → ReconciliacaoBarcode { tipo: TIMEOUT_NAO_RESOLVIDO }
          → Alerta enviado ao operador

T+10:30 - ADMIN verifica dashboard
          → Vê container preso há 10 horas
          → Investiga e descobre: DMT foi roubado
          → PUT /gate/reconciliacao/1/resolver
          → Resolução: "DMT roubado na madrugada, relatório de ocorrência aberto"
```

### Caso 2: Barcode Mismatch (Operador Errou)

```
T+00:15 - Operador DMT escaneia barcode incorreto
          → CONT-ERRADO em vez de CONT-CORRETO
          → Webhook: POST /gate/barcode/confirmar
          → GatePass.statusConfirmacao = CONFIRMADO
          → GatePass.codigoBarcode = CONT-ERRADO ❌

T+02:00 - Cronjob executa reconciliação
          → TosIntegrationService.obterStatusContainer()
          → TOS retorna: CONT-CORRETO
          → Detecta mismatch: local ≠ TOS
          → ReconciliacaoBarcode { tipo: BARCODE_MISMATCH }
          → barcodeEsperado = CONT-CORRETO
          → barcodeRecebido = CONT-ERRADO
          → Alerta: CRÍTICO

T+08:00 - Gerente investiga
          → Revisa logs do DMT
          → Vê que operador escaneou código errado
          → Localiza container correto no pátio
          → PUT /gate/reconciliacao/2/resolver
          → Resolução: "Operador treinado, container validado manualmente"
```

### Caso 3: Status Inconsistente (TOS Bloqueou Depois)

```
T+00:30 - Container liberado localmente
          → GatePass.status = LIBERADO
          → Gate abre, container entra

T+01:30 - TOS bloqueia container (inspeção aduaneira)
          → TOS status: RETIDO (gateLiberado = false)
          → But: GatePass.status = LIBERADO (não sincronizou)

T+02:00 - Cronjob executa reconciliação
          → Detecta: status local ≠ TOS
          → ReconciliacaoBarcode { tipo: STATUS_INCONSISTENTE }
          → statusLocal = LIBERADO
          → statusTos = RETIDO
          → Alerta: CRÍTICO (container dentro sem autorização)

T+09:00 - ADMIN atua
          → Container precisa sair para inspeção
          → Coordena com TOS para liberar
          → PUT /gate/reconciliacao/3/resolver
          → Resolução: "Container foi para inspeção aduaneira, liberado após validação"
```

## Testes

### Rodar Testes

```bash
mvn test -Dtest=ReconciliacaoBarcodeServiceTest
mvn test -Dtest=*Reconciliacao*
```

### Teste Manual

```bash
# Executar reconciliação agora (não esperar cronjob)
curl -X POST http://localhost:8082/gate/reconciliacao/executar \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json"

# Listar todos os problemas não resolvidos
curl -X GET http://localhost:8082/gate/reconciliacao/nao-resolvidas \
  -H "Authorization: Bearer $JWT_TOKEN"

# Filtrar por tipo
curl -X GET "http://localhost:8082/gate/reconciliacao/por-tipo?tipo=BARCODE_MISMATCH" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Resolver um problema
curl -X PUT http://localhost:8082/gate/reconciliacao/1/resolver \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"resolucao": "Container localizado e validado no pátio"}'
```

## Permissões

Apenas **ADMIN_PORTO** pode:
- Executar reconciliação manualmente
- Ver lista de desincronias
- Filtrar desincronias
- Resolver desincronias

```java
@PreAuthorize("hasRole('ADMIN_PORTO')")
public ResponseEntity<?> executarReconciliacao() { ... }
```

## Performance

### Cronjob Timing

- **Duração típica:** 2-5 minutos (em stack de 50k gate passes)
- **Consultas:** ~4 queries ao banco + múltiplas ao TOS
- **Índices:** Otimizados para evitar full table scans

### Carga no TOS

- **Máximo:** N containers verificados (N = GatePass.LIBERADO count)
- **Retry:** Circuit breaker se TOS indisponível
- **Cache:** TosIntegrationService aproveita cache existente

## Alertas (TODO)

Para a próxima iteração, implementar notificações:

```java
// NotificationGateway (já existe, aproveitar)
notificationGateway.enviarAlerta(
    "ADMIN_PORTO",
    "Desincronização de Barcode Detectada",
    "Container GP-001 aguardando confirmação há 45 minutos"
);
```

**Canais:**
- 📧 Email
- 💬 Slack / MS Teams
- 📱 SMS (crítico)
- 🔔 In-app notification

## Próximos Passos (Fase 4)

### Anomaly Detection Avançada
- Machine learning: detectar padrões anormais
- Alertas preditivos (antes de timeout)
- Análise de operadores com mais erros

### Analytics Dashboard
- Taxa de sucesso/rejeição de barcode
- Tempo P50/P95/P99 de confirmação
- Heatmap de desincronias por hora
- Correlação: latência rede vs timeout

### Automated Remediation
- Auto-resolver alguns tipos (ex: timeout com contingência habilitada)
- Retry automático de sincronizações
- Fallback para modo manual
