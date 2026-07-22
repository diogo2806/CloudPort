# Amarrados com referências de grupos distintos

## Finalidade

Representar o amarrado como uma unidade física íntegra durante a descarga, sem perder a rastreabilidade dos cargo lots e das referências comerciais que o compõem.

## Modelo operacional

- Um amarrado pertence a uma única visita de navio.
- Um amarrado possui um ou mais cargo lots vinculados.
- Cada cargo lot continua associado ao seu item de Bill of Lading e ao respectivo código de armazenagem.
- O amarrado é classificado automaticamente como misto quando possui mais de um código de armazenagem distinto.
- Um cargo lot não pode pertencer simultaneamente a dois amarrados íntegros.
- O vínculo não define o destino da carreta. A regra de direcionamento deve consumir esta informação em etapa posterior.

## Registrar um amarrado

`POST /api/carga-geral/amarrados`

```json
{
  "codigo": "AM-2026-0001",
  "visitaNavioId": "VISITA-123",
  "loteIds": [
    "11111111-1111-1111-1111-111111111111",
    "22222222-2222-2222-2222-222222222222"
  ]
}
```

A resposta apresenta:

- indicador `misto`;
- indicador `integro`;
- grupos de armazenagem distintos;
- quantidade de referências;
- cargo lot, Bill of Lading, sequência do item, descrição e grupo de cada referência.

## Consultas para planejamento e movimentação

- `GET /api/carga-geral/amarrados`: lista todos os amarrados.
- `GET /api/carga-geral/amarrados?visitaNavioId=VISITA-123`: lista os amarrados da descarga.
- `GET /api/carga-geral/amarrados?loteId=<uuid>`: localiza o amarrado de um cargo lot.
- `GET /api/carga-geral/amarrados/{id}`: retorna todas as referências do amarrado.

## Validações

A criação é rejeitada quando:

- o código do amarrado já existe;
- a requisição repete o mesmo cargo lot;
- algum cargo lot não existe;
- algum cargo lot já pertence a outro amarrado íntegro;
- os cargo lots não pertencem à visita de navio informada.

## Persistência

A migration `V14__amarrados_carga_multiplas_referencias.sql` cria:

- `amarrado_carga`, com identificação, visita, integridade e auditoria temporal;
- `amarrado_carga_lote`, com o vínculo entre o amarrado e todos os cargo lots;
- restrição única por cargo lot, impedindo perda de integridade ou associação ambígua.
