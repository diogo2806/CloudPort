# Validações de Yard Planning - CloudPort

## Visão Geral

O `ValidadorYardPlacementService` implementa validações de **curto prazo** para o sistema de planejamento de pátio e armazém, bloqueando alocações de contêineres que violem regras críticas de negócio.

## Validações Implementadas

### 1. **Compatibilidade de Carga com Berço**

Garante que o tipo de carga alocado seja suportado pela infraestrutura do berço de destino.

#### Tipos de Carga Validados:

| Tipo | Compatibilidade | Pré-Requisitos |
|------|-----------------|-----------------|
| **SECO** | `compatContainer` | Nenhum |
| **REFRIGERADO (REEFER)** | `compatReefer` + `energiaGenerica` | Infraestrutura de energia obrigatória |
| **PERIGOSO** | `compatPerigosa` | Área isolada (IMO compliance) |
| **GRANELEIRO** | `compatGranel` | Nenhum |
| **OUTRO** | `compatContainer` | Validação genérica |

#### Exemplo de Erro:

```
BerçoPorto_01 não suporta contêineres refrigerados (REEFER)
BerçoPorto_02 não dispõe de infraestrutura de energia para contêineres REEFER
BerçoPorto_03 não autoriza cargas perigosas (IMO)
```

### 2. **Limitação de Altura por Peso**

Previne instabilidade de pilhas ao limitar a altura máxima de empilhamento baseado no peso do contêiner.

#### Regras:

- **Peso ≤ 20t**: Altura máxima = **4 níveis**
- **20t < Peso ≤ 25t**: Altura máxima = **2 níveis**
- **Peso > 25t**: Altura máxima = **1 nível**

#### Exemplo:

```
Contêiner CNTR001 (peso: 26 toneladas)
Solicitação: Alocação no nível 2
Resultado: ❌ REJEITADO
Motivo: Pode ser empilhado apenas até nível 1
```

### 3. **Isolamento de Cargas Perigosas (IMO)**

Garante o isolamento obrigatório entre contêineres com cargas perigosas, respeitando regulamentações internacionais (IMO - International Maritime Organization).

#### Regra:

- Cargas PERIGOSO devem estar **isoladas de 1 bloco** (distância Chebyshev ≤ 1)
- Não pode haver outro contêiner PERIGOSO nas 8 posições adjacentes (linha±1, coluna±1)

#### Exemplo:

```
Posições Bloqueadas (❌) para nova carga PERIGOSO em (5,5):
  (4,4) (4,5) (4,6)
  (5,4) (5,5) (5,6)
  (6,4) (6,5) (6,6)

Posições Permitidas (✓): qualquer outra além destas
```

## Arquitetura

### Classes Principais

**`ValidadorYardPlacementService`**
- Orquestra todas as validações
- Injetado no `MapaPatioServico`
- Chamado antes de qualquer alocação/atualização
- Dispara `IllegalArgumentException` em caso de violação

**Fluxo de Validação:**

```
MapaPatioServico.registrarOuAtualizarConteiner()
  ↓
validadorYardPlacement.validarAlocacao(dto)
  ├─ validarCompatibilidadeCarga()
  ├─ validarAlturaPilha()
  └─ validarIsolamentoCargaPerigosa()
  ↓
[Prosseguir com alocação] ou [Lançar exceção]
```

## Testes Unitários

Localizados em: `ValidadorYardPlacementServiceTest.java`

Cobertura:
- ✅ Validação de compatibilidade de carga
- ✅ Rejeição de REEFER sem energia
- ✅ Rejeição de PERIGOSO em berços incompatíveis
- ✅ Limites de altura por peso
- ✅ Casos de sucesso e falha

**Executar testes:**

```bash
mvn test -Dtest=ValidadorYardPlacementServiceTest
```

## Próximos Passos (Médio/Longo Prazo)

### Fase 2: Otimização de Empilhamento

Implementar algoritmo de bin-packing 3D para **minimizar re-shuffles**:

- Análise de ETA (Estimated Time of Arrival) do navio
- Cálculo de sequência de embarque
- Prevenção de "digging" (container abaixo bloqueado)

**Serviço Proposto:** `OtimizadorYardPlacementService`

### Fase 3: Planejamento por Navio (Vessel Zoning)

Segmentação dinâmica de zonas do pátio por embarcação:

- Cálculo de distância ao berço de atracação
- Priorização automática de posições
- Minimização de tempo de transporte

**Serviço Proposto:** `ZoneadorYardPorNavio`

### Fase 4: Otimização de Rotas de Equipamento

Implementar dual-cycle optimization para RTG e Reach Stackers:

- Cálculo de trajetos mínimos
- Consolidação de múltiplas movimentações
- Redução de operações em vazio

**Serviço Proposto:** `OtimizadorRotasEquipamento`

## Integração com DTOs

### `ConteinerPatioRequisicaoDto`

Campos validados:
- `tipoCarga` → Compatibilidade com berço
- `destino` → Lookup de BercoPortuario
- `linha`, `coluna` → Isolamento IMO
- `camadaOperacional` → Limite de altura

### Resposta em Caso de Erro

Quando validação falha, o serviço retorna HTTP 400:

```json
{
  "erro": "Berço 'BERCO_01' não suporta contêineres refrigerados (REEFER)",
  "timestamp": "2026-06-02T14:30:00Z"
}
```

## Configuração

### Constantes Ajustáveis

Em `ValidadorYardPlacementService`:

```java
private static final Integer ALTURA_MAXIMA_EMPILHAMENTO_PADRAO = 4;
private static final BigDecimal PESO_LIMITE_EMPILHAMENTO = new BigDecimal("20");
```

Modificar conforme políticas do terminal.

## Performance

- ✅ O(1) para validação de compatibilidade (lookup por código)
- O(n) para isolamento IMO (scan de contêineres existentes)
  - Otimização futura: índice spatial ou cache de posições perigosas
- ✅ Validações executam antes de transação (fail-fast)

## Conformidade

- ✅ IMO Regulations (International Maritime Organization)
- ✅ Estabilidade Física de Pilhas
- ✅ Regulamentações de Contêineres Refrigerados (Power Supply)
- 🔄 Distâncias de Segurança (Perigosos) - Implementado versão básica

## Exemplo de Uso

```java
ConteinerPatioRequisicaoDto dto = new ConteinerPatioRequisicaoDto();
dto.setCodigo("CNTR123456");
dto.setTipoCarga("REFRIGERADO");
dto.setDestino("BERCO_NORTH_2");
dto.setLinha(10);
dto.setColuna(5);
dto.setCamadaOperacional("2");
dto.setStatus(StatusConteiner.ALOCADO);

try {
    validadorYardPlacement.validarAlocacao(dto);
    // Prosseguir com alocação
} catch (IllegalArgumentException e) {
    // Log erro e retornar ao cliente
    logger.error("Falha na validação: {}", e.getMessage());
}
```
