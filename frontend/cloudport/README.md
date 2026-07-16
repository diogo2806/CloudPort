# CloudPort Frontend

Portal principal do CloudPort, desenvolvido com React 19 e Vite 8.

## Escopo migrado

O runtime Angular foi removido. O portal React preserva:

- autenticação JWT e sessão compatível com o portal anterior;
- autorização por papéis;
- navegação dinâmica por `/api/navegacao/abas`, com menu de contingência;
- papéis, usuários, segurança, notificações e privacidade;
- telas operacionais de Gate, Ferrovia, Pátio, Navio e Embarque;
- integração SSO com o Control Room React por `postMessage` com origem restrita;
- `X-Correlation-Id` e contexto do usuário nos comandos operacionais;
- rotas públicas usadas anteriormente em `/home/*`.

## Integração com o backend

O backend está migrando para um monólito modular. O frontend consome uma única origem de API e não conhece hosts, portas ou nomes de módulos internos.

A URL pode apontar para o runtime consolidado, um proxy de transição ou a entrada única do ambiente. A arquitetura vigente está documentada em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Configuração dinâmica

A configuração é carregada de `public/assets/configuracao.json` antes da renderização:

```json
{
  "baseApiUrl": "http://localhost:8080",
  "navioControlRoomUrl": "http://localhost:8086"
}
```

- `baseApiUrl`: origem única para os contratos do portal;
- `navioControlRoomUrl`: endereço do Control Room incorporado por iframe e SSO.

Em produção, o arquivo pode ser substituído sem reconstruir os artefatos estáticos.

## Pré-requisitos

- Node.js 22
- npm 10+

## Instalação

```bash
npm install --no-audit --no-fund
```

## Desenvolvimento

```bash
npm start
```

A aplicação fica disponível em `http://localhost:4200`.

## Testes

```bash
npm test
```

Os testes unitários usam o test runner nativo do Node e cobrem sessão, papéis, erros, JWT, correlação e enriquecimento dos comandos operacionais.

## Build

```bash
npm run build
```

Os artefatos são gerados em `dist/cloudport`.

## Testes fim a fim

```bash
npm run e2e
```

O Playwright inicia o servidor Vite conforme `playwright.config.ts`.

## Regras para novos contratos

- usar caminhos relativos à `baseApiUrl`;
- manter autenticação e `X-Correlation-Id` no cliente compartilhado;
- não reproduzir no frontend a decisão entre chamada local e remota do backend;
- consumir erros padronizados com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp;
- preservar as rotas funcionais durante a migração do backend;
- não introduzir dependências Angular no portal React.
