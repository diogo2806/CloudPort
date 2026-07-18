# Exemplos da API da central de alertas

## Listar alertas ativos

`GET /api/v1/visibilidade/alertas/filtrados?status=ativo&page=0&size=50&sort=dataGerada,desc`

## Filtrar alertas críticos

`GET /api/v1/visibilidade/alertas/filtrados?status=ativo&severidade=critica`

## Resumo

`GET /api/v1/visibilidade/alertas/resumo`

## Reconhecer

`PATCH /api/v1/visibilidade/alertas/10/reconhecer`

```json
{
  "usuario": "operador"
}
```

## Resolver

`PATCH /api/v1/visibilidade/alertas/10/resolver`

```json
{
  "usuario": "operador"
}
```
