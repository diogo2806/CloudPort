# Exemplos Práticos - Yard Planning Validations

## Cenário 1: Alocação de Contêiner REEFER

### Requisição (POST /api/patio/conteineres)

```json
{
  "codigo": "REEFER001",
  "tipoCarga": "REFRIGERADO",
  "destino": "BERCO_ENERGY",
  "linha": 10,
  "coluna": 5,
  "camadaOperacional": "2",
  "status": "ALOCADO"
}
```

### Fluxo de Validação

1. **ValidadorYardPlacementService.validarAlocacao()** é chamado
2. **validarCompatibilidadeCarga():**
   - Lookup: BercoPortuario.findByCodigoIgnoreCase("BERCO_ENERGY")
   - Verifica: `compatReefer == true`
   - Verifica: `energiaGenerica == true`
3. **validarAlturaPilha():**
   - Camada 2 ≤ 4 ✅
   - Peso: null (contêiner não existe em DB) → sem restrição ✅
4. **validarIsolamentoCargaPerigosa():**
   - Tipo não é PERIGOSO → skip ✅

### Resposta (HTTP 200)

```json
{
  "id": 1,
  "codigo": "REEFER001",
  "linha": 10,
  "coluna": 5,
  "status": "ALOCADO",
  "tipoCarga": "REFRIGERADO",
  "destino": "BERCO_ENERGY",
  "camadaOperacional": "2"
}
```

---

## Cenário 2: Rejeição - REEFER sem Energia

### Requisição (POST /api/patio/conteineres)

```json
{
  "codigo": "REEFER002",
  "tipoCarga": "REFRIGERADO",
  "destino": "BERCO_SIMPLES",
  "linha": 15,
  "coluna": 3,
  "camadaOperacional": "1",
  "status": "ALOCADO"
}
```

### Fluxo de Validação

1. **validarCompatibilidadeCarga():**
   - Lookup: BercoPortuario.findByCodigoIgnoreCase("BERCO_SIMPLES")
   - Verifica: `compatReefer == true` ✅
   - Verifica: `energiaGenerica == false` ❌

### Resposta (HTTP 400)

```json
{
  "timestamp": "2026-06-02T14:45:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Berço 'BERCO_SIMPLES' não dispõe de infraestrutura de energia para contêineres REEFER",
  "path": "/api/patio/conteineres"
}
```

---

## Cenário 3: Limitação de Altura - Contêiner Pesado

### Requisição (POST /api/patio/conteineres)

Contêiner pré-existente em DB:
```sql
INSERT INTO conteiner (identificacao, tipo_carga, peso_toneladas, status_operacional)
VALUES ('HEAVY001', 'SECO', 26.500, 'LIBERADO');
```

Requisição de alocação:
```json
{
  "codigo": "HEAVY001",
  "tipoCarga": "SECO",
  "destino": "BERCO_01",
  "linha": 20,
  "coluna": 10,
  "camadaOperacional": "2",
  "status": "ALOCADO"
}
```

### Fluxo de Validação

1. **validarAlturaPilha():**
   - Camada 2 ≤ 4? Sim, inicialmente OK
   - Lookup: Conteiner.findByIdentificacaoIgnoreCase("HEAVY001")
   - Peso: 26.5t > 25t
   - Altura máxima para peso > 25t: 1 nível
   - 2 > 1 ❌

### Resposta (HTTP 400)

```json
{
  "timestamp": "2026-06-02T14:50:15Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Contêiner com peso 26.500 toneladas pode ser empilhado apenas até nível 1 (solicitado: nível 2)",
  "path": "/api/patio/conteineres"
}
```

### Solução - Realocação para Nível 1

```json
{
  "codigo": "HEAVY001",
  "tipoCarga": "SECO",
  "destino": "BERCO_01",
  "linha": 20,
  "coluna": 10,
  "camadaOperacional": "1",
  "status": "ALOCADO"
}
```

**Resultado:** HTTP 200 ✅

---

## Cenário 4: Isolamento de Cargas Perigosas

### Setup Inicial

Contêiner perigoso já alocado em (5, 5):

```sql
INSERT INTO posicao_patio (linha, coluna, camada_operacional) 
VALUES (5, 5, '1');

INSERT INTO carga_patio (codigo, descricao) 
VALUES ('PERIGOSO', 'Carga Perigosa - IMO');

INSERT INTO conteiner_patio (codigo, posicao_id, carga_id, status_conteiner, destino, atualizado_em)
VALUES ('DANGER001', 1, 1, 'ALOCADO', 'BERCO_SPEC', NOW());
```

### Requisição de Alocação Vizinha

```json
{
  "codigo": "DANGER002",
  "tipoCarga": "PERIGOSO",
  "destino": "BERCO_SPEC",
  "linha": 4,
  "coluna": 5,
  "camadaOperacional": "1",
  "status": "ALOCADO"
}
```

### Fluxo de Validação

1. **validarIsolamentoCargaPerigosa():**
   - Tipo é PERIGOSO
   - Buscar contêineres PERIGOSO próximos
   - Distância de (4,5) até (5,5): |4-5| = 1, |5-5| = 0
   - Existe contêiner PERIGOSO a distância ≤ 1 ❌

### Resposta (HTTP 400)

```json
{
  "timestamp": "2026-06-02T15:00:45Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cargas perigosas (IMO) devem estar isoladas. Há outra carga perigosa próxima à posição (4,5). Distância mínima: 1 bloco",
  "path": "/api/patio/conteineres"
}
```

### Mapa Visual - Bloqueado ❌

```
     3    4    5    6    7
  +----+----+----+----+----+
3 | OK | OK | OK | OK | OK |
  +----+----+----+----+----+
4 | OK | ❌ | ❌ | ❌ | OK |
  +----+----+----+----+----+
5 | OK | ❌ |DANG| ❌ | OK |
  +----+----+----+----+----+
6 | OK | ❌ | ❌ | ❌ | OK |
  +----+----+----+----+----+
7 | OK | OK | OK | OK | OK |
  +----+----+----+----+----+

DANGER001 em (5,5)
DANGER002 não pode ir em (4,5) [distância = 1]
```

### Solução - Alocação Distante

```json
{
  "codigo": "DANGER002",
  "tipoCarga": "PERIGOSO",
  "destino": "BERCO_SPEC",
  "linha": 3,
  "coluna": 3,
  "camadaOperacional": "1",
  "status": "ALOCADO"
}
```

**Distância até (5,5):** max(|3-5|, |3-5|) = 2 blocos ✅
**Resultado:** HTTP 200 ✅

---

## Cenário 5: Múltiplas Validações

### Requisição Problemática

```json
{
  "codigo": "PROBLEM001",
  "tipoCarga": "REFRIGERADO",
  "destino": "BERCO_INCOMP",
  "linha": 5,
  "coluna": 5,
  "camadaOperacional": "5",
  "status": "ALOCADO"
}
```

Problemas:
- `REFRIGERADO` em berço sem energia ❌
- `camadaOperacional: 5` > limite de 4 ❌

### Fluxo de Validação

1. **validarCompatibilidadeCarga():**
   - Falha na primeira validação
   - Dispara exceção imediatamente

### Resposta (HTTP 400)

```json
{
  "message": "Berço 'BERCO_INCOMP' não dispõe de infraestrutura de energia para contêineres REEFER"
}
```

**Nota:** A primeira validação falha; a altura não é validada. Isto é esperado (fail-fast pattern).

---

## Integração com Frontend

### Exemplo de Requisição via JavaScript

```javascript
async function alocarConteiner(dados) {
  try {
    const response = await fetch('/api/patio/conteineres', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        codigo: dados.codigo,
        tipoCarga: dados.tipoCarga,
        destino: dados.berco,
        linha: dados.linha,
        coluna: dados.coluna,
        camadaOperacional: String(dados.nivel),
        status: 'ALOCADO'
      })
    });

    if (!response.ok) {
      const erro = await response.json();
      console.error('Erro na alocação:', erro.message);
      // Exibir mensagem ao usuário
      mostrarNotificacao(erro.message, 'error');
      return null;
    }

    const resultado = await response.json();
    console.log('Contêiner alocado com sucesso:', resultado);
    mostrarNotificacao('Contêiner alocado com sucesso!', 'success');
    return resultado;

  } catch (e) {
    console.error('Erro de conexão:', e);
    mostrarNotificacao('Erro ao comunicar com servidor', 'error');
  }
}

// Uso:
alocarConteiner({
  codigo: 'CONT123456',
  tipoCarga: 'REFRIGERADO',
  berco: 'BERCO_ENERGY',
  linha: 10,
  coluna: 5,
  nivel: 2
});
```

---

## Testes Manuais

### Teste 1: API Curl

```bash
# REEFER em berço com energia (sucesso esperado)
curl -X POST http://localhost:8080/api/patio/conteineres \
  -H "Content-Type: application/json" \
  -d '{
    "codigo": "TEST001",
    "tipoCarga": "REFRIGERADO",
    "destino": "BERCO_ENERGY",
    "linha": 1,
    "coluna": 1,
    "camadaOperacional": "1",
    "status": "ALOCADO"
  }'

# REEFER em berço sem energia (erro esperado)
curl -X POST http://localhost:8080/api/patio/conteineres \
  -H "Content-Type: application/json" \
  -d '{
    "codigo": "TEST002",
    "tipoCarga": "REFRIGERADO",
    "destino": "BERCO_NOPOWER",
    "linha": 2,
    "coluna": 2,
    "camadaOperacional": "1",
    "status": "ALOCADO"
  }'
```

### Teste 2: Postman Collection

[Importar arquivo: `yard-planning-api.postman_collection.json`]

Endpoints:
- ✅ POST /api/patio/conteineres (novo)
- ✅ PUT /api/patio/conteineres/{id} (atualizar)
- ✅ GET /api/patio/mapa (consultar estado)
- ✅ GET /api/patio/movimentos (histórico)

---

## Troubleshooting

### "Berço não encontrado"
- Verificar código do berço em BD
- Validação passa sem erro se berço não existe (permite alocação genérica)

### "Distância de carga perigosa insuficiente"
- Visualizar mapa de pátio para ver posições ocupadas
- Afastar de pelo menos 1 bloco (8 células vizinhas)

### "Altura máxima excedida"
- Para contêiner > 25t: máximo nível 1
- Para contêiner 20-25t: máximo nível 2
- Realocação em nível inferior resolve
