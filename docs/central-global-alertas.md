# Central global de alertas

A central global consolida alertas operacionais de Gate, ferrovia, pátio, navio e embarque.

## Experiência operacional

O cabeçalho do portal apresenta o total de alertas ativos ou ainda não reconhecidos. O painel lateral permite:

- filtrar alertas ativos e resolvidos por severidade;
- reconhecer que um operador assumiu o acompanhamento;
- resolver a ocorrência;
- abrir diretamente o módulo relacionado;
- acessar a visão completa com indicadores e grade operacional;
- atualizar automaticamente os dados a cada 30 segundos.

## API

- `GET /api/v1/visibilidade/alertas/filtrados`
- `GET /api/v1/visibilidade/alertas/resumo`
- `PATCH /api/v1/visibilidade/alertas/{id}/reconhecer`
- `PATCH /api/v1/visibilidade/alertas/{id}/resolver`

As ações registram data e usuário responsável. A central completa está disponível em `/home/alertas`.
