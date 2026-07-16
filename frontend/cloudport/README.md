# CloudPort Frontend

Portal principal do CloudPort, desenvolvido com Angular 21.2.

## Integração com o backend

O backend está migrando para um monólito modular. O frontend deve consumir uma única origem de API e não deve conhecer hosts, portas ou nomes de módulos internos.

Durante a transição, a URL pode apontar para:

- o runtime consolidado;
- um proxy que encaminha rotas ainda legadas;
- uma entrada única de ambiente.

Não devem ser adicionadas URLs específicas como `servico-yard`, `servico-navio` ou `servico-gate` diretamente nos componentes e serviços Angular.

A arquitetura vigente está documentada em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Configuração dinâmica da URL da API

A URL base é definida em tempo de execução pelo arquivo `src/assets/configuracao.json`. O arquivo é carregado antes da inicialização do Angular e disponibilizado aos serviços por `ConfiguracaoAplicacaoService`.

Copie o exemplo:

```bash
cp src/assets/configuracao.exemplo.json src/assets/configuracao.json
```

Configure uma única `baseApiUrl`:

```json
{
  "baseApiUrl": "http://localhost:8086"
}
```

Em ambientes que ainda possuam deployments separados, configure `baseApiUrl` com a URL do proxy, não com a URL individual de um serviço.

Caso o arquivo não exista, esteja inacessível ou não contenha `baseApiUrl`, a aplicação interrompe o carregamento e apresenta a falha de configuração.

## Pré-requisitos

- Node.js 22
- npm compatível com o `package-lock.json`

## Instalação

```bash
npm ci
```

## Servidor de desenvolvimento

```bash
npm start
```

A aplicação fica disponível em `http://localhost:4200/`.

## Build

```bash
npm run build
```

Os artefatos são gerados em `dist/`.

## Testes unitários

```bash
npm test
```

## Testes fim a fim

Os testes e2e usam Playwright:

```bash
npm run e2e
```

## Regras para novos contratos

- usar caminhos relativos à `baseApiUrl`;
- manter autenticação e `X-Correlation-Id` nos interceptors compartilhados;
- não reproduzir no frontend a decisão entre chamada local e remota do backend;
- consumir erros padronizados com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp;
- preferir tipos gerados pelo OpenAPI consolidado quando essa geração estiver disponível;
- preservar as rotas funcionais durante a migração para evitar acoplamento à estrutura física do backend.
