# Validação de Barcode via Webhook DMT

## Visão Geral

Sistema bidirecional de validação de integridade de containers em cenários de alta latência (Wi-Fi/4G/5G). O gate envia uma solicitação ao dispositivo móvel do operador (DMT - Device Mobile Terminal), que confirma a leitura do código de barras do container via webhook.

## Arquitetura

```
┌─────────────────┐
│   GATE (TOS)    │
│   Servidor      │
└────────┬────────┘
         │
         │ 1. POST /gate/entrada
         │    + QR code/placa
         │
         ├─────────────────────────────────────────┐
         │                                         │
         │ 2. Validar com TOS (gate liberado,     │
         │    aduaneiro, documentação)             │
         │                                         │
         ├─────────────────────────────────────────┤
         │                                         │
         │ 3. Se gate.barcode.habilitado=true:    │
         │    - Gerar GatePass com token           │
         │    - Publicar solicitação no RabbitMQ   │
         │    - Agendar timeout handler            │
         │    - Retornar token ao cliente          │
         │                                         │
         └─────────────────────────────────────────┘
         │
         │ RabbitMQ (dmt-barcode-exchange)
         ├────────────────────────────────────────────────┐
         │                                                 │
    ┌────▼─────────────────────────────────────────┐     │
    │                                               │     │
    │   DMT (Dispositivo Móvel Operador)          │     │
    │   - Recebe solicitação de barcode            │     │
    │   - Operador escaneia código do container    │     │
    │   - Valida visualmente                       │     │
    │                                               │     │
    └────┬──────────────────────────────────────────┘     │
         │                                                 │
         │ 4. POST /gate/barcode/confirmar                │
         │    {                                            │
         │      tokenGatePass: "xyz",                      │
         │      codigoBarcode: "CONT123456",               │
         │      confirmado: true/false,                    │
         │      motivo: "...",                             │
         │      dispositivoDmtId: "DMT-001"                │
         │    }                                            │
         │                                                 │
         └────────────────────────────────────────────────┘
         │
         ├─────────────────────────────────────────┐
         │                                         │
         │ 5. Processar confirmação:               │
         │    - Validar token                      │
         │    - Atualizar GatePass status          │
         │    - Registrar evento                   │
         │    - Liberar ou reter container         │
         │                                         │
         └─────────────────────────────────────────┘
         │
         │ 6. Resposta ao DMT
         │ 7. Publicar evento de atualização real-time
```

## Fluxo de Confirmação

### Caso 1: Confirmação com Sucesso
```
1. GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
2. Solicitar confirmação ao DMT (RabbitMQ)
3. Operador confirma: barcode correto
4. Webhook atualiza:
   - codigoBarcode = "CONT123456"
   - statusConfirmacaoBarcode = CONFIRMADO
   - GatePass.status = LIBERADO
   - Registra evento com timestamp
5. Container liberado para entrada
```

### Caso 2: Rejeição de Barcode
```
1. GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
2. Solicitar confirmação ao DMT
3. Operador rejeita: "Barcode não corresponde ao esperado"
4. Webhook atualiza:
   - codigoBarcode = "CONT-INCORRETO"
   - statusConfirmacaoBarcode = REJEITADO
   - motivoRejeicaoBarcode = "Barcode não corresponde..."
   - GatePass.status = RETIDO
5. Container bloqueado, gera alerta de discrepância
```

### Caso 3: Timeout de Confirmação
```
1. GatePass.status = AGUARDANDO_CONFIRMACAO_BARCODE
2. Timer de 30s inicia (configurável)
3. Se DMT não responder em 30s:
   - POST /gate/barcode/timeout é chamado
   - statusConfirmacaoBarcode = TIMEOUT
   - GatePass.status = RETIDO
   - Alerta operador do gate
   - Se falhar-sem-confirmacao=false: container entra com warning
   - Se falhar-sem-confirmacao=true: container é bloqueado
```

## Endpoints

### 1. POST `/gate/barcode/confirmar`

**Webhook chamado pelo DMT para confirmar/rejeitar leitura de barcode**

**Autenticação:** Bearer Token (JWT)

**Requisição:**
```json
{
  "tokenGatePass": "string (required) - UUID do gate pass",
  "codigoBarcode": "string (required) - Código lido pelo DMT",
  "confirmado": "boolean (required) - true=confirmado, false=rejeitado",
  "motivo": "string (optional) - Motivo da rejeição",
  "dataConfirmacao": "ISO-8601 datetime (optional) - Timestamp da confirmação",
  "dispositivoDmtId": "string (required) - ID do dispositivo que confirmou"
}
```

**Resposta (200 OK):**
```json
{
  "gatePassId": 123,
  "tokenGatePass": "xyz",
  "codigoBarcode": "CONT123456",
  "statusConfirmacao": "CONFIRMADO|REJEITADO|TIMEOUT",
  "dataConfirmacao": "2026-06-02T15:30:00",
  "mensagem": "Barcode confirmado com sucesso"
}
```

**Erros:**
- `404 Not Found` - Token não encontrado
- `400 Bad Request` - Gate pass já foi finalizado ou já foi confirmado
- `401 Unauthorized` - Sem autenticação
- `422 Unprocessable Entity` - Campos obrigatórios faltando

---

### 2. POST `/gate/barcode/timeout`

**Notificar que o DMT não respondeu no tempo esperado**

**Autenticação:** Bearer Token + ADMIN_PORTO role

**Requisição:**
```json
{
  "tokenGatePass": "string (required)",
  "dispositivoDmtId": "string (required)"
}
```

**Resposta (202 Accepted):**
```json
{
  "gatePassId": 123,
  "tokenGatePass": "xyz",
  "statusConfirmacao": "TIMEOUT",
  "dataConfirmacao": "2026-06-02T15:31:00",
  "mensagem": "Timeout na confirmação de barcode do dispositivo DMT"
}
```

---

## Configuração

### application.properties

```properties
# Habilitar/desabilitar validação de barcode
gate.barcode.habilitado=${GATE_BARCODE_HABILITADO:false}

# Timeout aguardando confirmação (Duration format)
gate.barcode.timeout-confirmacao=${GATE_BARCODE_TIMEOUT:PT30S}

# Se true: rejeita container se timeout. Se false: libera com warning
gate.barcode.falhar-sem-confirmacao=${GATE_BARCODE_FALHAR_SEM_CONFIRMACAO:false}

# RabbitMQ para enviar solicitações ao DMT
dmt.rabbitmq.exchange=${DMT_RABBITMQ_EXCHANGE:dmt-barcode-exchange}
dmt.rabbitmq.routing-key=${DMT_RABBITMQ_ROUTING_KEY:barcode.confirmacao.solicitacao}
```

### .env (Docker Compose)

```bash
GATE_BARCODE_HABILITADO=true
GATE_BARCODE_TIMEOUT=PT30S
GATE_BARCODE_FALHAR_SEM_CONFIRMACAO=false

DMT_RABBITMQ_EXCHANGE=dmt-barcode-exchange
DMT_RABBITMQ_ROUTING_KEY=barcode.confirmacao.solicitacao
```

## Data Model

### GatePass (estendido)

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `codigoBarcode` | String(50) | Código lido pelo DMT |
| `dataConfirmacaoBarcode` | DateTime | Quando DMT confirmou |
| `statusConfirmacaoBarcode` | Enum | PENDENTE, CONFIRMADO, REJEITADO, TIMEOUT |
| `motivoRejeicaoBarcode` | String(500) | Motivo se rejeitado |

### Enum: StatusConfirmacaoBarcode

```java
PENDENTE         // Aguardando DMT responder
CONFIRMADO       // Barcode validado com sucesso
REJEITADO        // Operador rejeitou (barcode incorreto)
TIMEOUT          // DMT não respondeu no tempo
```

### Enum: StatusGate (novo status)

```java
AGUARDANDO_CONFIRMACAO_BARCODE  // Aguardando confirmação do DMT
```

## Auditoria e Logging

### Logs de Sucesso

```
event=barcode.confirmado gatePassId=123 barcode=CONT123456 dmt=DMT-001 timestamp=2026-06-02T15:30:00
```

### Logs de Erro

```
event=barcode.rejeitado gatePassId=123 barcode=CONT-INCORRETO dmt=DMT-001 motivo="Barcode não corresponde" timestamp=2026-06-02T15:31:00

event=barcode.timeout gatePassId=123 dmt=DMT-001 timestamp=2026-06-02T15:31:00
```

### GateEvent registrado

Cada confirmação/rejeição gera um `GateEvent` com:
- `status` = LIBERADO ou RETIDO
- `observacao` = "Barcode confirmado pelo DMT-001: CONT123456"
- `usuarioResponsavel` = "sistema-barcode-validation"
- `registradoEm` = timestamp da confirmação

## Tratamento de Latência

### Cenário: Wi-Fi lento (latência 5-10s)

```
T+0s:  Gate envia solicitação de barcode
T+2s:  Mensagem chega ao DMT via RabbitMQ
T+5s:  Operador lê barcode (delay de UI/rede)
T+7s:  DMT envia confirmação
T+10s: Webhook processa (total: 10s < timeout 30s) ✓ OK
```

### Cenário: 4G intermitente (latência 15-25s)

```
T+0s:  Gate envia solicitação
T+8s:  RabbitMQ entrega ao DMT (intermitência)
T+20s: Operador confirma
T+30s: Webhook recebe (total: 30s = timeout) ⚠️ CRÍTICO
T+31s: Timeout handler executa (deve verificar antes)
```

**Mitigação:** Aumentar `gate.barcode.timeout-confirmacao` em redes lentas.

## Integração com GateFlowService

A integração será feita em fase 2:

```java
// GateFlowService.registrarEntrada() - Future
public GateDecisionDTO registrarEntrada(GateFlowRequest request) {
    // ... validações existentes ...
    
    if (barcodeProperties.isHabilitado()) {
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.PENDENTE);
        gatePassRepository.save(gatePass);
        
        dmtBarcodeService.solicitarConfirmacaoBarcode(gatePass, 
            agendamento.getCodigo());
        
        // Retornar resposta pendente com token para DMT
        return GateDecisionDTO.pendenteBarcodeConfirmacao(gatePass);
    }
    
    // Fluxo atual (sem barcode)
    gatePass.setStatus(StatusGate.LIBERADO);
    // ... resto do fluxo ...
}
```

## Próximas Etapas

### Fase 2: Reconciliação Periódica
- Cronjob noturno comparando estado local vs TOS
- Detectar containers "presos" (entrou sem sair)
- Alertar desincronias de barcode

### Fase 3: Anomaly Detection
- Múltiplos containers mesma placa em 1h
- Container saiu sem entrada registrada
- Tempo de gate > 30min (suspeito)

### Fase 4: Analytics Dashboard
- Taxa de sucesso/rejeição de barcode
- Tempo médio de confirmação (P50, P95, P99)
- Dispositivos DMT com mais erros
- Correlação latência rede vs timeout

## Troubleshooting

### Problema: Webhook retorna 404 (token não encontrado)

**Causa:** Token expirou ou foi inválido desde o início
**Solução:** Verificar token no GatePass antes do timeout (cron job)

### Problema: RabbitMQ não entrega mensagem ao DMT

**Verificar:**
```bash
# RabbitMQ logs
docker logs rabbitmq

# Verificar exchange e routing key
rabbitmqctl list_exchanges
rabbitmqctl list_bindings

# Re-configurar se necessário em RabbitConfiguracao.java
```

### Problema: Timeout muito curto (muitos false positives)

**Solução:** Aumentar `gate.barcode.timeout-confirmacao` em application.properties
```properties
gate.barcode.timeout-confirmacao=PT60S  # 60 segundos
```

## Testes

### Rodar testes unitários

```bash
mvn test -Dtest=ConfirmacaoBarcodeServiceTest
mvn test -Dtest=BarcodeConfirmacaoControllerTest
```

### Testar webhook manualmente

```bash
# Confirmar barcode com sucesso
curl -X POST http://localhost:8082/gate/barcode/confirmar \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokenGatePass": "abc-xyz-123",
    "codigoBarcode": "CONT123456",
    "confirmado": true,
    "dispositivoDmtId": "DMT-001"
  }'

# Rejeitar barcode
curl -X POST http://localhost:8082/gate/barcode/confirmar \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokenGatePass": "abc-xyz-123",
    "codigoBarcode": "CONT-ERRADO",
    "confirmado": false,
    "motivo": "Barcode não corresponde ao esperado",
    "dispositivoDmtId": "DMT-001"
  }'

# Registrar timeout
curl -X POST http://localhost:8082/gate/barcode/timeout \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokenGatePass": "abc-xyz-123",
    "dispositivoDmtId": "DMT-001"
  }'
```

## Referências

- [StatusGate enum](../backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/enums/StatusGate.java)
- [GatePass model](../backend/servico-gate/src/main/java/br/com/cloudport/servicogate/model/GatePass.java)
- [ConfirmacaoBarcodeService](../backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/ConfirmacaoBarcodeService.java)
- [BarcodeConfirmacaoController](../backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/gestor/BarcodeConfirmacaoController.java)
- [Database Migration V3](../backend/servico-gate/src/main/resources/db/migration/V3__adicionar_confirmacao_barcode.sql)
