# Control Room Navio + Pátio

Frontend React do módulo operacional de navios siderúrgicos do CloudPort.

## Stack

- React 19
- Vite 8
- Testes com o runner nativo do Node.js
- API REST do runtime de Navio

## Execução local

```bash
npm install
npm start
```

A aplicação inicia em `http://localhost:4201`.

## Build

```bash
npm run build
```

Os arquivos são gerados em `dist/servico-navio-siderurgico`, caminho consumido pelos builds Maven e Docker do backend.

## Configuração em runtime

O arquivo `public/assets/configuracao.json` define:

- `baseApiUrl`: URL base das APIs. Vazio utiliza a mesma origem da aplicação.
- `trustedParentOrigins`: origens autorizadas a enviar a sessão do portal pelo `postMessage`.

## Autenticação

O Control Room aceita login próprio e SSO do portal principal. No fluxo incorporado, a aplicação publica `CLOUDPORT_CONTROL_ROOM_READY` e recebe `CLOUDPORT_AUTH_SESSION`. Somente usuários com `ROLE_ADMIN_PORTO`, `ROLE_PLANEJADOR` ou `ROLE_OPERADOR_GATE` acessam a área operacional.

## Sequências operacionais de guindaste (BUS1150)

O plano salvo apresenta um painel de execução para cada alocação persistida. O identificador operacional é `crane-plan-{idDaAlocacao}` e a work queue do Yard é usada como unidade de reconciliação.

Endpoints:

- `POST /api/crane-sequences`: cria ou recupera uma sequência idempotente por `movementId`.
- `POST /api/crane-sequences/{movementId}/start`: inicia ou retoma uma sequência.
- `POST /api/crane-sequences/{movementId}/pause`: pausa uma sequência iniciada e exige motivo.
- `POST /api/crane-sequences/{movementId}/finish`: finaliza uma sequência iniciada.
- `POST /api/crane-sequences/{movementId}/cancel`: cancela uma sequência não finalizada e exige motivo.
- `GET /api/crane-sequences/{movementId}`: consulta o estado atual.
- `GET /api/crane-sequences/{movementId}/history`: consulta transições e alertas de reconciliação.
- `GET /api/crane-sequences?vesselVisitId=&status=&from=&to=`: pesquisa por visita, estado e janela de início planejado.

As transições incompatíveis e os conflitos de versão retornam `409 Conflict`. `STARTED` e `FINISHED` geram registros idempotentes na outbox. O job periódico compara sequências em execução ou finalizadas com a work queue correspondente no Yard, registra divergências na trilha de auditoria e não altera o estado operacional automaticamente.

## Testes

```bash
npm test
```
