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

## Testes

```bash
npm test
```
