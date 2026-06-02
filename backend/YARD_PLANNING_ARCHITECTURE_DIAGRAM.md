# Yard Planning Architecture Diagrams

Visualizações da arquitetura do sistema de Planejamento de Pátio.

## 1. Arquitetura em Duas Camadas

```
┌────────────────────────────────────────────────────────────┐
│                       CLIENTE                              │
│          (REST API / Frontend / TOS Integration)           │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ [1] POST /api/patio/otimizacao/alocar
                 │     + Lista de Containers (codigo, ETA, peso, tipo)
                 │
                 ▼
        ┌─────────────────────────────┐
        │   CAMADA 1: OTIMIZAÇÃO      │
        │  OptimizadorYardService     │
        │                             │
        │  ┌─────────────────────┐    │
        │  │ 1. Ordenar por ETA  │    │
        │  │ (mais cedo = topo)  │    │
        │  └─────────────────────┘    │
        │           │                 │
        │           ▼                 │
        │  ┌─────────────────────┐    │
        │  │ 2. Bin-Packing 3D   │    │
        │  │ (aloca posições)    │    │
        │  └─────────────────────┘    │
        │           │                 │
        │           ▼                 │
        │  ┌─────────────────────┐    │
        │  │ 3. Vessel Zoning    │    │
        │  │ (zona de embarque)  │    │
        │  └─────────────────────┘    │
        │                             │
        └─────────────┬───────────────┘
                      │
        [2] Posições otimizadas (linha, coluna, nível)
                      │
                      ▼
        ┌─────────────────────────────┐
        │  CAMADA 2: VALIDAÇÃO        │
        │ ValidadorYardPlacementSvc   │
        │                             │
        │  ┌─────────────────────┐    │
        │  │ 1. Compatibilidade  │    │
        │  │ (REEFER + energia)  │    │
        │  └─────────────────────┘    │
        │           │                 │
        │           ▼                 │
        │  ┌─────────────────────┐    │
        │  │ 2. Altura/Peso      │    │
        │  │ (limites de stack)  │    │
        │  └─────────────────────┘    │
        │           │                 │
        │           ▼                 │
        │  ┌─────────────────────┐    │
        │  │ 3. Isolamento IMO   │    │
        │  │ (cargas perigosas)  │    │
        │  └─────────────────────┘    │
        │                             │
        └─────────────┬───────────────┘
                      │
                      │ [3] Resultado: VALIDADO ou REJEITADO
                      │
                      ▼
        ┌─────────────────────────────┐
        │  PERSISTÊNCIA               │
        │  MapaPatioServico           │
        │  registrarOuAtualizarCont   │
        │                             │
        │  ✅ Salvar no banco         │
        │  ✅ Registrar movimento     │
        │  ✅ Publicar evento         │
        └─────────────┬───────────────┘
                      │
                      ▼
        ┌─────────────────────────────┐
        │  DATABASE                   │
        │  - conteiner_patio          │
        │  - posicao_patio            │
        │  - movimento_patio          │
        └─────────────────────────────┘
```

## 2. Fluxo de Validação Detalhado

```
INPUT: ConteinerPatioRequisicaoDto
├─ codigo: "CONT001"
├─ tipoCarga: "REFRIGERADO"
├─ destino: "BERCO_ENERGY"
├─ linha: 0, coluna: 0, camada: "1"
└─ peso: 18 toneladas
            │
            ▼
    [VALIDAÇÃO 1: COMPATIBILIDADE]
    │
    ├─ Tipo = REFRIGERADO?
    │  └─ Buscar BercoPortuario por destino
    │     ├─ compatReefer = true ✅
    │     └─ energiaGenerica = true ✅
    │
    └─ Se FALHA: IllegalArgumentException
                └─ "Berço não suporta REEFER"
                   HTTP 400 Bad Request
            │
            ✅ PASSOU
            │
            ▼
    [VALIDAÇÃO 2: ALTURA/PESO]
    │
    ├─ Camada 1 ≤ 4 máximo? ✅
    │
    ├─ Buscar Conteiner por código
    │  └─ peso: 18t
    │
    ├─ 18t < 20t? Altura máx = 4 ✅
    │
    └─ Se FALHA: IllegalArgumentException
                └─ "Peso > 25t: máx nível 1"
                   HTTP 400 Bad Request
            │
            ✅ PASSOU
            │
            ▼
    [VALIDAÇÃO 3: ISOLAMENTO IMO]
    │
    ├─ Tipo = PERIGOSO? Não, é REEFER ✅ SKIP
    │
    └─ Se FALHA: IllegalArgumentException
                └─ "Carga perigosa próxima"
                   HTTP 400 Bad Request
            │
            ✅ PASSOU TODAS
            │
            ▼
OUTPUT: ConteinerMapaDto
├─ id: 1
├─ codigo: "CONT001"
├─ linha: 0, coluna: 0
├─ nivel: 1
└─ HTTP 200 OK + JSON response
```

## 3. Fluxo do Bin-Packing 3D + ETA Sorting

```
INPUT: [Container1(ETA:14h), Container2(ETA:10h), Container3(ETA:12h)]

[PASSO 1: SORT POR ETA]
│
├─ Container2 (ETA:10h) → Sequência 0
├─ Container3 (ETA:12h) → Sequência 1
└─ Container1 (ETA:14h) → Sequência 2
│
▼
Ordenado: [CONT2, CONT3, CONT1]
(mais cedo primeiro)

[PASSO 2: BIN-PACKING 3D]

Grid: 20×20×4 (1.600 posições total)

Aloca CONT2:
┌─────────────────┐
│                 │
│  Procura Grid   │ (0,0,1) Disponível? SIM
│                 │ Distância = 0+0 = 0
│                 │ → Marcar ocupado
└─────────────────┘
CONT2 → (0, 0, 1) ✅

Aloca CONT3:
┌─────────────────┐
│                 │
│  Procura Grid   │ (0,0,2) Disponível? SIM
│                 │ Embaixo tem CONT2? SIM
│                 │ → Marcar ocupado
└─────────────────┘
CONT3 → (0, 0, 2) ✅

Aloca CONT1:
┌─────────────────┐
│                 │
│  Procura Grid   │ (0,0,3) Disponível? SIM
│                 │ Embaixo tem CONT3? SIM
│                 │ → Marcar ocupado
└─────────────────┘
CONT1 → (0, 0, 3) ✅

[RESULTADO FINAL]

Bloco (0,0):
┌─────────────┐
│  CONT1      │ ← ETA 14h (embaixo)
├─────────────┤
│  CONT3      │ ← ETA 12h
├─────────────┤
│  CONT2      │ ← ETA 10h (TOPO - sai primeiro)
└─────────────┘

SEM RE-SHUFFLES! 🎉
```

## 4. Estados e Transições

```
┌──────────────────────────────────────────────────────┐
│         CICLO DE VIDA DO CONTAINER NO PÁTIO          │
└──────────────────────────────────────────────────────┘

┌─────────────┐
│   CHEGADA   │ Container entra no porto
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│   [ALOCAÇÃO NÃO-OTIMIZADA]          │
│                                     │
│  MapaPatioServico.registrar()       │
│  ├─ Validar (compatibilidade)      │
│  ├─ Aloca posição (manual/rand)    │
│  └─ STATUS: ALOCADO                │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│   [ALOCAÇÃO OTIMIZADA]              │
│                                     │
│  OptimizadorYard.otimizar()         │
│  ├─ Sort por ETA                   │
│  ├─ Bin-packing 3D                │
│  └─ Resultado: posição candidata   │
│                                     │
│  ValidadorYardPlacement.validar()   │
│  ├─ Compatibilidade ✅             │
│  ├─ Altura/Peso ✅                 │
│  └─ Isolamento ✅                  │
│                                     │
│  MapaPatioServico.persistir()       │
│  └─ STATUS: OTIMIZADO              │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│   ARMAZENAMENTO                     │
│   (Aguardando embarque)             │
└──────┬──────────────────────────────┘
       │
       │ (Hora ETA aproxima-se)
       │
       ▼
┌─────────────────────────────────────┐
│   EMBARQUE                          │
│   RTG/Reach Stacker move container  │
│   → Navio                           │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│   SAÍDA                             │
│   Container deixa terminal          │
│   STATUS: EMBARCADO                 │
└─────────────────────────────────────┘
```

## 5. Distribuição Espacial: Vessel Zoning

```
Vista Aérea do Pátio - Navio "MSC GULSEUM"

BERÇO ATRACAÇÃO
       │
    Coluna 0    Coluna 1    Coluna 2    Coluna 3
    (dist:0)    (dist:1)    (dist:2)    (dist:3)

Linha 0: [BLK-1]   [BLK-2]   [BLK-3]   [BLK-4]
        ✅ZONA     ✅ZONA    ✅ZONA    ✅ZONA
        (dist:0)   (dist:1)  (dist:2)  (dist:3)

Linha 1: [BLK-5]   [BLK-6]   [BLK-7]   [BLK-8]
        ✅ZONA     ✅ZONA    ✅ZONA    ✅ZONA
        (dist:1)   (dist:2)  (dist:3)  (dist:4)

Linha 2: [BLK-9]  [BLK-10]  [BLK-11]  [BLK-12]
        ✅ZONA    ✅ZONA    ✅ZONA    ❌FORA
        (dist:2)  (dist:3)  (dist:4)  (dist:5)

Linha 3: [BLK-13] [BLK-14]  [BLK-15]  ❌FORA
        ✅ZONA    ✅ZONA    ❌FORA
        (dist:3)  (dist:4)  (dist:5)

...

LIMITE: distanciaMaximaAoBerco = 4 blocos

Blocos Alocados (✅):
┌────────────┐  ┌────────────┐  ┌────────────┐
│  BLK-1     │  │  BLK-2     │  │  BLK-3     │
│ CONT[1,2,3]│  │ CONT[4,5]  │  │ CONT[6]    │
│ (dist: 0)  │  │(dist: 1)   │  │(dist: 2)   │
└────────────┘  └────────────┘  └────────────┘

Tempo de Transporte:
- BLK-1: 2 min → BERÇO
- BLK-2: 3 min → BERÇO
- BLK-3: 4 min → BERÇO
```

## 6. Timeline de Embarque: Com vs Sem Otimização

### SEM Otimização (Alocação Manual)

```
Hora:     Operação:              Ação:
────────────────────────────────────────────────
10:00     CONT001 embarque       RTG pega CONT001
          (ETA prevista)         Status: ✅ OK
          
10:30     CONT008 embarque       CONT008 embaixo de CONT003
          (ETA prevista)         RTG move CONT003 (RE-SHUFFLE)
                                 RTG pega CONT008
                                 RTG recoloca CONT003
                                 Status: ❌ CUSTO $150
                                 
11:00     CONT004 embarque       RTG pega CONT004
          (ETA prevista)         Status: ✅ OK
          
11:30     CONT008 embarque       JÁ EMBARQUE
          (ETA prevista)         Nenhuma ação
          
12:00     CONT002 embarque       CONT002 embaixo de CONT007
          (ETA prevista)         RTG move CONT007 (RE-SHUFFLE)
                                 RTG pega CONT002
                                 RTG recoloca CONT007
                                 Status: ❌ CUSTO $150
                                 
...

TOTAL RE-SHUFFLES: 4
CUSTO: $600 USD
TEMPO PERDIDO: 20 min
```

### COM Otimização (Bin-Packing + ETA)

```
Hora:     Operação:              Ação:
────────────────────────────────────────────────
10:00     CONT001 embarque       RTG pega CONT001 (topo)
          (ETA prevista)         Status: ✅ OK
          
10:30     CONT004 embarque       RTG pega CONT004 (topo)
          (ETA prevista)         Status: ✅ OK
          
11:00     CONT008 embarque       RTG pega CONT008 (topo)
          (ETA prevista)         Status: ✅ OK
          
11:30     CONT008 embarque       JÁ EMBARCADO
          (ETA prevista)         Nenhuma ação
          
12:00     CONT002 embarque       RTG pega CONT002 (topo)
          (ETA prevista)         Status: ✅ OK
          
...

TOTAL RE-SHUFFLES: 0
CUSTO: $0 USD (-100%)
TEMPO PERDIDO: 0 min (-100%)
```

## 7. Máquina de Estados - Alocação

```
                    ┌─────────────────┐
                    │  START: Input   │
                    └────────┬────────┘
                             │
                             ▼
                 ┌─────────────────────┐
                 │  OTIMIZAR           │
                 │  (Bin-Packing 3D)   │
                 └────────┬────────────┘
                          │
                          ▼
            ┌──────────────────────────┐
            │  VALIDAÇÃO 1:            │
            │  Compatibilidade         │
            └─┬────────────────────────┘
              │
              ├─ SIM ──→ [PASSOU 1] ──┐
              │                        │
              └─ NÃO ──→ [ERRO] ──→ ❌ REJECT
                                       │
                                       └─→ OUTPUT: REJEITADO
              ┌──────────────────────────┐
              │  VALIDAÇÃO 2:            │
              │  Altura/Peso             │
              └─┬────────────────────────┘
                │
                ├─ SIM ──→ [PASSOU 2] ──┐
                │                        │
                └─ NÃO ──→ [ERRO] ──→ ❌ REJECT

              ┌──────────────────────────┐
              │  VALIDAÇÃO 3:            │
              │  Isolamento IMO          │
              └─┬────────────────────────┘
                │
                ├─ SIM ──→ [PASSOU 3] ──┐
                │                        │
                └─ NÃO ──→ [ERRO] ──→ ❌ REJECT

                        ┌─────────────────────┐
                        │  PERSISTIR          │
                        │  (Save to Database) │
                        └────────┬────────────┘
                                 │
                                 ▼
                        ┌─────────────────────┐
                        │  OUTPUT: SUCESSO    │
                        │  ✅ ALOCADO         │
                        └─────────────────────┘
```

## 8. Matriz de Decisão - Validação

```
┌──────────────────────────────────────────────────────────┐
│  MATRIZ DE VALIDAÇÃO                                     │
├──────────────────┬──────────┬──────────┬──────────────────┤
│  Cenário         │ Compat.  │ Altura   │ Isolamento │ Resultado
├──────────────────┼──────────┼──────────┼────────────┼──────────────┤
│ SECO em          │ ✅       │ ✅       │ ✅         │ ✅ ACEITO    │
│ berço container  │          │          │            │              │
├──────────────────┼──────────┼──────────┼────────────┼──────────────┤
│ REEFER sem       │ ❌       │ -        │ -          │ ❌ REJEITADO │
│ energia          │          │          │            │              │
├──────────────────┼──────────┼──────────┼────────────┼──────────────┤
│ Pesado (26t)     │ ✅       │ ❌       │ ✅         │ ❌ REJEITADO │
│ no nível 2       │          │ nivel>1  │            │              │
├──────────────────┼──────────┼──────────┼────────────┼──────────────┤
│ PERIGOSO perto   │ ✅       │ ✅       │ ❌         │ ❌ REJEITADO │
│ de PERIGOSO      │          │          │ distância  │              │
│                  │          │          │ < 2 blocos │              │
├──────────────────┼──────────┼──────────┼────────────┼──────────────┤
│ Tudo OK          │ ✅       │ ✅       │ ✅         │ ✅ ACEITO    │
└──────────────────┴──────────┴──────────┴────────────┴──────────────┘
```

---

**Diagrama Version:** 1.0  
**Last Updated:** 2026-06-02
