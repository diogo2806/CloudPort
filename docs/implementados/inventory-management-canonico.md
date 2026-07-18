# Inventory Management canônico

## Objetivo

O módulo de pátio passa a possuir um domínio único para representar qualquer unidade física controlada pelo terminal. A unidade deixa de ser tratada apenas como uma linha de contêiner no mapa e passa a concentrar ciclo de vida, equipamento, condição, ownership, documentos, restrições e histórico operacional.

A modelagem foi inspirada no conceito de unidade e equipamento do N4, incluindo relacionamentos entre equipamentos com papéis operacionais, inspector da unidade, tipos equivalentes, lacres, documentos, avarias, holds, permissions, manutenção e controle reefer.

## Unidades suportadas

- contêiner;
- chassi;
- carreta;
- acessório, incluindo genset;
- combinações montadas e desmontadas entre unidades.

## Modelo principal

### `unidade_inventario`

Mantém:

- identificação e prefixo;
- tipo e categoria do equipamento;
- ciclo de vida;
- condição física;
- status de manutenção;
- proprietário e operador;
- posição real e posição planejada;
- peso bruto;
- versão otimista e timestamps.

### Coleções da unidade

- `unidade_lacre`;
- `unidade_documento`;
- `unidade_avaria`;
- `unidade_restricao`;
- `unidade_manutencao`;
- `unidade_historico_atributo`;
- `unidade_reefer_registro`.

### Referências de equipamento

- `tipo_equipamento_inventario`: categoria, ISO, dimensões, tara, capacidade, indicador reefer e grupo de equivalência;
- `prefixo_equipamento_inventario`: prefixo, proprietário e categoria;
- `vinculo_equipamento`: montagem entre unidade principal e unidade relacionada.

Os papéis de vínculo disponíveis são:

- `PRIMARIO`;
- `TRANSPORTE`;
- `PAYLOAD`;
- `ACESSORIO`;
- `ACESSORIO_NO_CHASSI`.

### Inventário físico

`contagem_inventario_fisico` registra lote, unidade, posição esperada, posição lida, responsável e divergência. São identificadas divergências de posição, unidade não localizada e unidade não prevista. A divergência permanece auditável após a resolução.

## Ciclo de vida

Estados canônicos:

1. `PRE_AVISADA`;
2. `ATIVA`;
3. `NO_PATIO`;
4. `EM_OPERACAO`;
5. `EM_TRANSITO`;
6. `EMBARCADA`;
7. `DESEMBARCADA`;
8. `LIBERADA`;
9. `DESPACHADA`;
10. `INATIVA`;
11. `APOSENTADA`.

O serviço valida a matriz de transição. Unidade com hold ativo não pode avançar para `LIBERADA` ou `DESPACHADA`.

## API

Base: `/yard/inventario/canonico`

### Unidade

- `GET /unidades`;
- `GET /unidades/{unidadeId}`;
- `POST /unidades`;
- `PATCH /unidades/{unidadeId}/estado`;
- `PATCH /unidades/{unidadeId}/propriedade`;
- `PATCH /unidades/{unidadeId}/posicao`.

### Dados associados

- `POST /unidades/{unidadeId}/lacres`;
- `POST /unidades/{unidadeId}/documentos`;
- `POST /unidades/{unidadeId}/avarias`;
- `POST /unidades/{unidadeId}/holds-permissions`;
- `POST /unidades/{unidadeId}/manutencoes`;
- `POST /unidades/{unidadeId}/reefer`.

### Referências e montagem

- `GET|POST /tipos`;
- `GET|POST /prefixos`;
- `POST /montagens`;
- `POST /montagens/{vinculoId}/desmontagem`.

### Inventário físico

- `POST /contagens`;
- `GET /divergencias`;
- `POST /divergencias/{divergenciaId}/resolucao`.

## Interface

A rota `/home/patio/inventario` agora apresenta:

- filtros por identificação, categoria, estado, condição, proprietário, operador, hold e reefer;
- indicadores por categoria e por exceção operacional;
- tabela operacional com busca, ordenação, filtros, paginação, exportação e colunas configuráveis;
- cadastro de contêiner, chassi, carreta e acessório;
- inspector unificado ao selecionar uma unidade;
- tipos, dimensões e equivalências;
- lacres;
- documentos;
- avarias e componentes;
- holds e permissions;
- manutenção e reparo;
- equipamentos montados e desmontados;
- leituras reefer;
- histórico de atributos;
- ações rápidas para ciclo de vida, lacre e restrição.

## Compatibilidade com o inventário existente

A migração `V200__inventory_management_canonico.sql` importa os registros de `conteiner_patio` para `unidade_inventario`. O serviço também sincroniza contêineres legados ainda não convertidos quando o inventário canônico é consultado.

O endpoint legado `/yard/inventario` foi mantido para compatibilidade. A interface principal utiliza o novo contrato canônico.

## Validações principais

- identificação única;
- tipo ativo obrigatório;
- dimensões positivas;
- pesos não negativos;
- validade de hold/permission consistente;
- bloqueio de liberação com hold ativo;
- transição de ciclo de vida válida;
- montagem sem autoassociação ou duplicidade;
- papel `TRANSPORTE` limitado a chassi ou carreta;
- papel de acessório limitado à categoria `ACESSORIO`;
- leitura reefer limitada a tipo refrigerado;
- umidade entre 0 e 100%;
- inventário físico classificado de forma determinística.

## Testes adicionados

- bloqueio de liberação com hold;
- transição inválida de ciclo de vida;
- validação de categoria na montagem;
- divergência de posição no inventário físico;
- normalização de filtros do frontend;
- contrato de consulta e detalhe da unidade canônica.
