# CloudPort Frontend

Portal principal do CloudPort, desenvolvido com React 19 e Vite 8.

## Escopo disponível

O runtime Angular foi removido. O portal React preserva autenticação, autorização e navegação e concentra as interfaces operacionais dos módulos.

### Componentes compartilhados

- autenticação JWT e sessão compatível com o portal anterior;
- autorização por papéis;
- navegação dinâmica por `/api/navegacao/abas`, com menu de contingência;
- cliente HTTP comum com JWT e `X-Correlation-Id`;
- tratamento do contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp;
- central global de alertas no cabeçalho e em `/home/alertas`;
- ajuda contextual por rota, módulo, papel e processo;
- layout responsivo e navegação por teclado.

### Grade operacional

O componente `OperationalDataGrid` é usado pelas páginas que antes exibiam tabelas simples.

Ele oferece:

- busca rápida em todas as colunas;
- filtros combináveis por coluna;
- ordenação de texto, número e data;
- paginação local e contrato opcional para paginação no backend;
- ocultação, reordenação e congelamento de colunas;
- layouts e filtros salvos no navegador;
- seleção múltipla e ações em lote;
- inspector lateral do registro;
- exportação CSV e Excel;
- neutralização de fórmulas em arquivos exportados;
- suporte a teclado e atributos de acessibilidade.

### Telas operacionais

- Gate visual com pistas, filas, calendário, capacidade, jornada, OCR, balança, inspeção, documentos, avarias, EIR e SLA;
- Rail com composição gráfica do trem, vagões, linhas, ocupação, progresso, conflitos, drag-and-drop e line-up vertical;
- Yard com Google Maps, blocos, scan, seção, microvisão, heatmaps, CHEs, reefers, rotas, allocations, restrições e simulação;
- Navio com line-up vertical, Vessel Planner em múltiplas vistas, drag-and-drop, overlays e Quay Monitor;
- Inventory Management com unidade, equipamento, documentos, avarias, manutenção, holds, permissions, reefer e inventário físico;
- Carga Geral com Bill of Lading, itens, lotes, referências, movimentações, avarias e consolidação;
- Billing e CAP com tarifas, cobranças, faturas, pagamentos e visão da transportadora;
- Control Room React incorporado por iframe e SSO restrito por origem;
- Visibilidade e rastreamento operacional;
- administração, usuários, papéis, segurança, notificações e privacidade.

## Integração com o backend

O frontend consome uma única origem de API e não conhece hosts, portas ou nomes de módulos internos.

A URL pode apontar para o runtime consolidado, um proxy de transição ou a entrada única do ambiente. A arquitetura vigente está em [`../../docs/arquitetura-monolito-modular.md`](../../docs/arquitetura-monolito-modular.md).

## Configuração dinâmica

A configuração é carregada de `public/assets/configuracao.json` antes da renderização:

```json
{
  "baseApiUrl": "http://localhost:8080",
  "navioControlRoomUrl": "http://localhost:8080",
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
- `googleMaps.apiKey`: chave de navegador da Maps JavaScript API;
- `googleMaps.mapId`: identificador opcional de estilo do mapa;
- `googleMaps.center`: ponto de ancoragem usado para desenhar o terminal;
- `googleMaps.zoom` e `mapTypeId`: aproximação inicial e camada base;
- `slotWidthMeters` e `slotLengthMeters`: dimensões do polígono de uma pilha;
- `stackGapMeters`, `blockGapMeters` e `blockColumns`: espaçamento e distribuição visual dos blocos;
- `rotationDegrees`: rotação do conjunto para alinhar o desenho às vias e aos blocos.

Quando `googleMaps.apiKey` está vazia, o mapa externo não é carregado e a grade operacional permanece disponível.

A chave do Google Maps é entregue ao navegador. Restrinja-a por referenciador HTTP e limite seu uso à Maps JavaScript API.

Em produção, `assets/configuracao.json` pode ser substituído sem reconstruir o frontend.

## Pré-requisitos

- Node.js 22;
- npm 10+.

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

Os testes unitários usam o test runner nativo do Node e cobrem sessão, papéis, erros, JWT, correlação, grade operacional, exportações, alertas, ajuda contextual, contratos operacionais, Gate, Rail, Yard e Vessel Planner.

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

## EasyPanel

Use uma aplicação GitHub com:

- repositório: `diogo2806/CloudPort`;
- branch: `main`;
- caminho de build: `/frontend`;
- arquivo: `Dockerfile`;
- porta: `80`;
- health check: `/health`.

O `frontend/Dockerfile` usa Node 22 no estágio de build e Nginx no estágio final. O `frontend/nginx.conf` entrega os arquivos estáticos, expõe `/health` e usa fallback para `index.html` nas rotas da SPA.

## Regras para novos contratos

- usar caminhos relativos à `baseApiUrl`;
- manter autenticação e `X-Correlation-Id` no cliente compartilhado;
- não reproduzir no frontend a decisão entre chamada local e remota do backend;
- consumir erros padronizados;
- preservar as rotas funcionais durante a migração do backend;
- não introduzir dependências Angular;
- reutilizar `OperationalDataGrid`, `PageHeader`, ajuda contextual e central de alertas antes de criar componentes paralelos;
- manter alternativa acessível para interações baseadas em drag-and-drop.
