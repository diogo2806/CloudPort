# CloudPort Frontend

Portal principal do CloudPort, desenvolvido com React 19 e Vite 8.

## Escopo migrado

O runtime Angular foi removido. O portal React preserva:

- autenticação JWT e sessão compatível com o portal anterior;
- autorização por papéis;
- navegação dinâmica por `/api/navegacao/abas`, com menu de contingência;
- papéis, usuários, segurança, notificações e privacidade;
- telas operacionais de Gate, Ferrovia, Pátio, Navio e Embarque;
- mapa operacional do pátio com grade de contingência e desenho georreferenciado no Google Maps;
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
  "navioControlRoomUrl": "http://localhost:8086",
  "googleMaps": {
    "apiKey": "",
    "mapId": "",
    "center": {
      "lat": -22.93315,
      "lng": -43.83731
    },
    "zoom": 19,
    "mapTypeId": "satellite",
    "slotWidthMeters": 3.2,
    "slotLengthMeters": 12.2,
    "stackGapMeters": 1,
    "blockGapMeters": 24,
    "blockColumns": 2,
    "rotationDegrees": 0
  }
}
```

- `baseApiUrl`: origem única para os contratos do portal;
- `navioControlRoomUrl`: endereço do Control Room incorporado por iframe e SSO;
- `googleMaps.apiKey`: chave de navegador da Maps JavaScript API. Quando vazia, o mapa externo não é carregado e a grade operacional permanece disponível;
- `googleMaps.mapId`: identificador opcional de estilo do mapa;
- `googleMaps.center`: ponto de ancoragem usado para desenhar o pátio. O valor versionado aponta para a região portuária de Itaguaí e deve ser substituído pelas coordenadas reais do terminal;
- `googleMaps.zoom` e `mapTypeId`: aproximação inicial e camada base;
- `slotWidthMeters` e `slotLengthMeters`: dimensões do polígono de uma pilha;
- `stackGapMeters`, `blockGapMeters` e `blockColumns`: espaçamento e distribuição visual dos blocos;
- `rotationDegrees`: rotação do conjunto para alinhar o desenho às vias e aos blocos da imagem de satélite.

A chave do Google Maps é entregue ao navegador e não deve ser tratada como segredo de backend. No Google Cloud, restrinja-a por referenciador HTTP aos domínios do CloudPort e limite sua utilização à Maps JavaScript API. O projeto da chave precisa ter faturamento habilitado.

Em produção, o arquivo pode ser substituído sem reconstruir os artefatos estáticos. Isso permite ajustar chave, coordenadas, escala e rotação por terminal.

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

Os testes unitários usam o test runner nativo do Node e cobrem sessão, papéis, erros, JWT, correlação, contratos operacionais e geometria do mapa do pátio.

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
