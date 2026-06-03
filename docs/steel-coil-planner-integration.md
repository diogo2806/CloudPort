# Steel Coil Planner - Integracao Comercial

Este documento descreve os dois modos de uso do modulo de planejamento de embarque de bobinas.

## 1. Uso standalone por planilha

O cliente pode operar o modulo sem integracao tecnica inicial. O planejador acessa a tela `Navio > Steel Coil Planner`, importa um arquivo `.xlsx` ou `.csv`, gera o plano automatico e faz os ajustes manuais.

### Colunas aceitas

| Coluna | Obrigatoria | Descricao |
| --- | --- | --- |
| `coil_id` | Nao | Identificador unico da bobina. |
| `codigo` | Nao | Codigo comercial ou lote. Se ausente, `coil_id` e usado como codigo. |
| `quantidade` | Nao | Quantidade de bobinas iguais na linha. Default: 1. |
| `peso_toneladas` | Sim | Peso unitario em toneladas. |
| `largura_mm` | Nao | Largura da bobina em milimetros. Tambem aceita metros. |
| `diametro_externo_mm` | Nao | Diametro externo em milimetros. Tambem aceita metros. |
| `porto_descarga` | Nao | Porto de descarga usado para sequencia LIFO. |
| `cliente` | Nao | Cliente final ou consignatario. |
| `grade_aco` | Nao | Grade, familia ou especificacao do aco. |
| `observacoes` | Nao | Observacoes operacionais. |

### Regras de importacao

- CSV e XLSX usam a primeira aba/primeira linha como cabecalho.
- Cabecalhos sao normalizados: acentos, espacos e caixa nao importam.
- Dimensoes acima de 20 sao tratadas como milimetros e convertidas para metros.
- Linhas com `peso_toneladas` vazio ou invalido sao ignoradas.
- Linhas com mesmo codigo, destino, peso e dimensoes sao agrupadas.
- Um novo upload limpa o plano atual para evitar mistura de manifestos.

## 2. Integracao via API

Para clientes com TOS, ERP, WMS ou sistema de shipping, o modulo pode receber dados por API e devolver o plano calculado.

### Endpoints recomendados

```http
POST /api/steel-coils/plans
POST /api/steel-coils/plans/{planId}/coils/import
POST /api/steel-coils/plans/{planId}/auto-plan
GET  /api/steel-coils/plans/{planId}
GET  /api/steel-coils/plans/{planId}/export.xlsx
GET  /api/steel-coils/plans/{planId}/export.pdf
```

### Payload de importacao

```json
{
  "vessel": {
    "name": "MV Example",
    "voyage": "V001",
    "holds": 5
  },
  "ports": ["Santos", "Rio Grande", "Vitoria", "Cartagena"],
  "coils": [
    {
      "coilId": "C-1001",
      "code": "HRC-A",
      "quantity": 1,
      "weightT": 24.5,
      "widthMm": 1550,
      "outerDiameterMm": 1850,
      "dischargePort": "Cartagena",
      "customer": "Cliente A",
      "steelGrade": "SAE1006"
    }
  ]
}
```

### Resposta de planejamento

```json
{
  "planId": "plan-123",
  "status": "DRAFT",
  "summary": {
    "totalCoils": 84,
    "totalWeightT": 1832.4,
    "gmM": 1.72,
    "trimM": 0.18,
    "shearForcePercent": 42,
    "bendingMomentPercent": 51
  },
  "positions": [
    {
      "coilId": "C-1001",
      "hold": 3,
      "row": 4,
      "tier": 1,
      "xM": 12.4,
      "yM": -2.1,
      "zM": 0.8,
      "alerts": []
    }
  ],
  "alerts": [
    {
      "code": "HEAVY_COIL_LASHING",
      "severity": "WARN",
      "message": "Bobinas acima de 25 t exigem cintas adicionais."
    }
  ]
}
```

## Modelo de licenca

- `Standalone`: importacao de planilha, planejamento, ajustes manuais e exportacao.
- `Integrated`: standalone mais API para TOS/ERP/WMS.
- `Enterprise`: instalacao on-premise, SSO, auditoria e regras especificas do armador/navio.
