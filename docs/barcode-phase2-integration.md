# Fase 2: Integração com GateFlowService

## Resumo

Integração completa da validação de barcode no fluxo de entrada do gate. O barcode é agora **bloqueante** - container não entra até operador DMT confirmar a leitura.

## Novo Fluxo de Entrada

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Operador Gate (Frontend)                        │
│                      POST /gate/entrada (QR ou placa)                  │
└────────────────────────────────┬────────────────────────────────────────┘
                                 │
                    ┌────────────▼──────────────┐
                    │  GateFlowService          │
                    │  registrarEntrada()       │
                    └────────────┬──────────────┘
                                 │
                    ┌────────────▼──────────────────────┐
                    │ 1. Validar com TOS                │
                    │ 2. Validar documentos             │
                    │ 3. Validar janela de tempo        │
                    │ 4. Registrar dataEntrada          │
                    │ 5. Setar status EM_PROCESSAMENTO  │
                    └────────────┬─────────────────────┘
                                 │
                    ┌────────────▼──────────────────────────────┐
                    │ if barcode.habilitado=true:              │
                    │   - Status = AGUARDANDO_CONFIRMACAO      │
                    │   - StatusConfirmacao = PENDENTE         │
                    │   - Publicar solicitação → DMT (RabbitMQ)│
                    │   - Registrar evento                     │
                    │   - Retornar PENDENTE com token          │
                    │ else:                                    │
                    │   - Status = LIBERADO                    │
                    │   - Registrar evento                     │
                    │   - Retornar LIBERADO                    │
                    └────────────┬──────────────────────────────┘
                                 │
                    ┌────────────▼──────────────┐
                    │ AgendamentoRealtimeService│
                    │ notificarStatus()         │
                    │ (WebSocket → Frontend)    │
                    └──────────────────────────┘
                                 │
        ┌────────────────────────▼─────────────────────┐
        │ Frontend atualiza UI: "Aguardando barcode"   │
        │ Exibe token para passar ao DMT               │
        └──────────────────────────────────────────────┘
                                 │
                        ┌────────▼────────┐
                        │  DMT (Operador)  │
                        │ Recebe token     │
                        │ Escaneia código  │
                        │ Confirma/Rejeita │
                        └────────┬────────┘
                                 │
            ┌────────────────────▼──────────────────────┐
            │ POST /gate/barcode/confirmar              │
            │ {                                         │
            │   tokenGatePass: "xxx",                   │
            │   codigoBarcode: "CONT123456",            │
            │   confirmado: true/false,                 │
            │   dispositivoDmtId: "DMT-001"             │
            │ }                                         │
            └────────────┬───────────────────────────────┘
                         │
            ┌────────────▼──────────────────┐
            │ ConfirmacaoBarcodeService     │
            │ confirmarBarcode()            │
            │ ou                            │
            │ rejeitarBarcode()             │
            │ ou                            │
            │ registrarTimeoutBarcode()     │
            └────────────┬──────────────────┘
                         │
            ┌────────────▼──────────────────────────┐
            │ if confirmado:                        │
            │   - Status = LIBERADO                 │
            │   - StatusConfirmacao = CONFIRMADO    │
            │   - Registrar barcode e timestamp     │
            │ else if rejeitado:                    │
            │   - Status = RETIDO                   │
            │   - StatusConfirmacao = REJEITADO     │
            │   - Registrar motivo                  │
            │ else if timeout:                      │
            │   - Status = RETIDO                   │
            │   - StatusConfirmacao = TIMEOUT       │
            │   - Alerta operador                   │
            └────────────┬───────────────────────────┘
                         │
            ┌────────────▼──────────────────────┐
            │ AgendamentoRealtimeService        │
            │ notificarStatus()                 │
            │ (WebSocket → Frontend)            │
            └────────────┬──────────────────────┘
                         │
        ┌────────────────▼────────────────────────────┐
        │ Frontend atualiza em tempo real             │
        │ Mostra: LIBERADO ✓ ou RETIDO ✗             │
        │ Se LIBERADO: abre cancela do gate          │
        │ Se RETIDO: alerta e bloqueia acesso        │
        └──────────────────────────────────────────────┘
```

## Comparação: Antes vs Depois

### Antes (Sem Barcode)

```
POST /gate/entrada
    ↓
[Validar TOS + docs + janela]
    ↓
Status = LIBERADO
    ↓
Resposta: 200 OK (autorizado)
    ↓
Gate abre imediatamente
```

**Problema:** Sem validação que container físico realmente passou

### Depois (Com Barcode)

```
POST /gate/entrada
    ↓
[Validar TOS + docs + janela]
    ↓
Status = AGUARDANDO_CONFIRMACAO_BARCODE
    ↓
Resposta: 200 OK com token (pendente)
    ↓
DMT operador escaneia barcode
    ↓
POST /gate/barcode/confirmar
    ↓
Status = LIBERADO ou RETIDO
    ↓
Real-time notifica frontend
    ↓
Gate abre (LIBERADO) ou bloqueia (RETIDO)
```

**Benefício:** Sincronização entre espelho digital e pátio físico ✓

## GateDecisionDTO - Nova Estrutura

### Resposta quando barcode está desabilitado (anterior)

```json
{
  "autorizado": true,
  "statusGate": "LIBERADO",
  "statusDescricao": "Liberado",
  "agendamentoId": 123,
  "codigoAgendamento": "AG-001",
  "gatePassId": 456,
  "codigoGatePass": "GP-001-XYZABC",
  "mensagem": "Entrada liberada com sucesso"
}
```

### Resposta quando barcode está habilitado (novo)

```json
{
  "autorizado": false,
  "statusGate": "AGUARDANDO_CONFIRMACAO_BARCODE",
  "statusDescricao": "Aguardando confirmação de barcode",
  "agendamentoId": 123,
  "codigoAgendamento": "AG-001",
  "gatePassId": 456,
  "codigoGatePass": "GP-001-XYZABC",
  "tokenGatePass": "550e8400-e29b-41d4-a716-446655440000",
  "mensagem": "Aguardando confirmação de barcode do operador DMT. Token enviado para dispositivo móvel."
}
```

**Nota:** `autorizado=false` indica que container **não** está liberado ainda

## Configuração

### Habilitar/Desabilitar Barcode

```properties
# application.properties
gate.barcode.habilitado=true    # ativa o fluxo com barcode
```

### Timeouts

```properties
# Quanto tempo aguardar confirmação do DMT
gate.barcode.timeout-confirmacao=PT30S

# Se timeout: bloquear container ou liberar com warning?
# true  = rejeitar container se timeout
# false = liberar container com log (segurança reduzida)
gate.barcode.falhar-sem-confirmacao=false
```

## GatePass - Campos Estendidos

| Campo | Type | Descrição |
|-------|------|-----------|
| `codigoBarcode` | String | Barcode confirmado pelo DMT |
| `dataConfirmacaoBarcode` | DateTime | Quando DMT confirmou |
| `statusConfirmacaoBarcode` | Enum | PENDENTE / CONFIRMADO / REJEITADO / TIMEOUT |
| `motivoRejeicaoBarcode` | String | Razão se rejeitado |

## Auditoria e Logging

### Entrada com Barcode

```
event=entrada.registrada gatePassId=1 status=AGUARDANDO_CONFIRMACAO_BARCODE token=550e8400-e29b-41d4-a716-446655440000
```

### Confirmação Bem-sucedida

```
event=barcode.confirmado gatePassId=1 barcode=CONT123456 dmt=DMT-001 timestamp=2026-06-02T15:30:00
```

### Rejeição

```
event=barcode.rejeitado gatePassId=1 barcode=CONT-INCORRETO dmt=DMT-001 motivo="Barcode não corresponde ao esperado" timestamp=2026-06-02T15:31:00
```

### Timeout

```
event=barcode.timeout gatePassId=1 dmt=DMT-001 timestamp=2026-06-02T15:31:00
```

## Real-time Updates via WebSocket

Após confirmação/rejeição/timeout, o sistema publica evento em tempo real:

```
Channel: agendamento.status
Event: {
  "agendamentoId": 123,
  "codigoAgendamento": "AG-001",
  "statusGate": "LIBERADO|RETIDO|TIMEOUT",
  "codigoBarcode": "CONT123456|null",
  "dataConfirmacao": "2026-06-02T15:30:00"
}
```

**Frontend:**
- Inscreve-se no canal `agendamento.{id}.status`
- Recebe atualizações em tempo real
- Atualiza UI automaticamente
- Se LIBERADO: abre gate
- Se RETIDO: exibe alerta e bloqueia

## Testes

### Rodar testes de integração

```bash
mvn test -Dtest=GateFlowBarcodeIntegrationTest
mvn test -Dtest=ConfirmacaoBarcodeServiceTest
mvn test -Dtest=ConfirmacaoBarcodeControllerTest
```

### Testar fluxo end-to-end

1. **Habilitar barcode em application.properties:**
```properties
gate.barcode.habilitado=true
```

2. **Iniciar stack com Docker Compose:**
```bash
docker-compose -f docker/docker-compose.yml up --build
```

3. **Registrar entrada:**
```bash
curl -X POST http://localhost:8082/gate/entrada \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "qrCode": "AG-001"
  }'
```

Resposta esperada:
```json
{
  "autorizado": false,
  "statusGate": "AGUARDANDO_CONFIRMACAO_BARCODE",
  "tokenGatePass": "550e8400-e29b-41d4-a716-446655440000",
  "mensagem": "Aguardando confirmação de barcode..."
}
```

4. **DMT confirma barcode (após 5s):**
```bash
curl -X POST http://localhost:8082/gate/barcode/confirmar \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tokenGatePass": "550e8400-e29b-41d4-a716-446655440000",
    "codigoBarcode": "CONT123456",
    "confirmado": true,
    "dispositivoDmtId": "DMT-001"
  }'
```

5. **Frontend recebe real-time update:**
```javascript
// WebSocket listener
socket.on('agendamento.123.status', (event) => {
  console.log('Gate status:', event.statusGate); // LIBERADO
  openGate(); // Abre a cancela
});
```

## Database Migration

Migration V3 adiciona colunas:

```sql
ALTER TABLE gate_pass
ADD COLUMN codigo_barcode VARCHAR(50),
ADD COLUMN data_confirmacao_barcode TIMESTAMP,
ADD COLUMN status_confirmacao_barcode VARCHAR(40),
ADD COLUMN motivo_rejeicao_barcode VARCHAR(500);
```

Índices para performance:
```sql
CREATE INDEX idx_gate_pass_token ON gate_pass(token);
CREATE INDEX idx_gate_pass_status_confirmacao ON gate_pass(status_confirmacao_barcode);
```

## Arquitetura Resumo

```
GateFlowService
├─ if barcode.habilitado
│  ├─ DmtBarcodeService.solicitarConfirmacaoBarcode()
│  │  └─ RabbitMqDmtPublisher (publica solicitação)
│  ├─ Retorna GateDecisionDTO.pendenteBarcodeConfirmacao()
│  └─ AgendamentoRealtimeService.notificarStatus()
│
└─ BarcodeConfirmacaoController (webhook)
   ├─ POST /gate/barcode/confirmar
   ├─ ConfirmacaoBarcodeService.confirmarBarcode()
   ├─ Atualiza GatePass.status (LIBERADO ou RETIDO)
   └─ AgendamentoRealtimeService.notificarStatus()
```

## Próximos Passos (Fase 3)

### Reconciliação Periódica
- Cronjob noturno: detectar containers "presos"
- Comparar GatePass.statusConfirmacao vs TOS
- Alertar desincronias

### Anomaly Detection
- Múltiplos containers mesma placa em 1h
- Container saiu sem entrada
- Tempo confirmação > threshold

### Dashboard Analytics
- Taxa sucesso/rejeição de barcode
- Tempo P50/P95/P99 de confirmação
- Dispositivos DMT com mais erros
- Correlação latência vs timeout
