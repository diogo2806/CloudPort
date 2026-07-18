# CloudPort Frontend

Portal principal do CloudPort, desenvolvido com React 19 e Vite 8.

## Escopo disponível

O runtime Angular foi removido. O portal React concentra as interfaces operacionais e administrativas do CloudPort.

### Estrutura compartilhada

- autenticação JWT e sessão compatível com o portal anterior;
- autorização por papéis;
- navegação dinâmica por `/api/navegacao/abas`, com menu de contingência;
- papéis, usuários, segurança, notificações e privacidade;
- cliente HTTP comum com JWT, contexto do usuário e `X-Correlation-Id`;
- tratamento do contrato de erro com `codigo`, `mensagem`, `detalhes`, `correlationId` e timestamp;
- integração SSO com o Control Room React por `postMessage` e origem restrita;
- central global de alertas disponível no cabeçalho e em `/home/alertas`;
- ajuda contextual por rota, módulo, processo e perfil;
- layout responsivo, navegação por teclado e atributos de acessibilidade.

### OperationalDataGrid

As páginas que antes exibiam tabelas simples usam a grade operacional compartilhada.

O componente oferece:

- busca rápida em todas as colunas, sem diferenciar acentos ou caixa;
- filtros combináveis por coluna;
- ordenação de texto, número e data;
- paginação local e contrato opcional para paginação no backend;
- ocultação, exibição, reordenação e congelamento de colunas;
- preferências e visões nomeadas persistidas no navegador;
- seleção múltipla e ações em lote;
- inspector lateral do registro;
- exportação CSV e Excel;
- neutralização de valores que possam ser interpretados como fórmulas em planilhas;
- navegação por teclado e `aria-sort` nos cabeçalhos.

### Ajuda contextual

O `PageHeader` apresenta ajuda correspondente à rota e ao módulo atual.

O painel inclui:

- finalidade da tela;
- fluxo recomendado;
- campos e informações principais;
- permissões;
- estados do processo;
- bloqueios e validações;
- exemplo operacional;
- atalhos e links relacionados.

Atalhos disponíveis: `F1`, `Shift + ?` e `Esc`.

### Telas operacionais

#### Gate

- quadro visual de pistas e filas por estágio;
- calendário de agendamentos e ocupação por janela;
- jornada do veículo com OCR, balança, inspeção e liberação;
- documentos, imagens, avarias, trouble transactions e SLA;
- impressão e reimpressão de EIR;
- bookings, Bill of Lading, EDO, ERO, IDO e pré-avisos;
- controle de entrada, presença e saída de pessoas;
- Billing e portal CAP.

#### Ferrovia

- visitas, manifestos, vagões, contêineres e ordens de trabalho;
- composição gráfica da locomotiva e dos vagões;
- associação e progresso de carga e descarga por vagão;
- linhas ferroviárias, ocupação e conflitos;
- drag-and-drop com alternativa acessível por seletor;
- line-up ferroviário vertical.

#### Pátio e inventário

- mapa georreferenciado no Google Maps e grade de contingência;
- vistas de bloco, seção, scan e microvisão;
- heatmaps de ocupação e dwell time;
- workspaces, notas, bloqueios, interdições e permissões;
- movimentação simulada e confirmada;
- telemetria de CHEs e reefers;
- rotas e editor gráfico de allocations;
- inventário canônico de contêineres, chassis, carretas e acessórios;
- lacres, documentos, avarias, manutenção, holds, ownership, montagem e inventário físico.

#### Navio e Vessel Planner

- visitas, itens operacionais, planos e line-up interno;
- line-up público em `/line-up`;
- profile, top, section e tier views sincronizadas;
- drag-and-drop, restow, IMDG, reefer, OOG e peso por stack;
- inspector de slot, alertas e sequência de guindastes;
- Quay Monitor, crane plan e produtividade;
- overlays operacionais de estabilidade, força estrutural e risco indicativo de lashing.

#### Carga geral

- Bill of Lading, itens e cargo lots;
- carga geral, projeto e break-bulk;
- commodities, embalagens, produtos e códigos de manuseio;
- perigosos, temperatura, avarias, estoque e movimentações;
- consolidação, desconsolidação e vínculos logísticos.

#### Control Room e Visibilidade

- work queues, job lists, dispatch e transições de work instruction;
- equipamentos, telemetria, dispositivos, comandos e indisponibilidades;
- atualização por SSE com reconexão e fallback;
- alertas globais com reconhecimento e resolução;
- rastreamento e histórico de contêineres;
- navegação para o módulo relacionado ao evento ou alerta.

## Integração com o backend

O frontend consome uma única origem de API e não conhece hosts, portas ou nomes de módulos internos.

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
- `googleMaps.apiKey`: chave de navegador da Maps JavaScript API;
- `googleMaps.mapId`: identificador opcional de estilo do mapa;
- `googleMaps.center`: ponto de ancoragem usado para desenhar o pátio;
- `googleMaps.zoom` e `mapTypeId`: aproximação inicial e camada base;
- `slotWidthMeters` e `slotLengthMeters`: dimensões do polígono de uma pilha;
- `stackGapMeters`, `blockGapMeters` e `blockColumns`: espaçamento e distribuição visual dos blocos;
- `rotationDegrees`: rotação do conjunto para alinhar o desenho às vias e aos blocos.

Quando `googleMaps.apiKey` está vazia, o mapa externo não é carregado e a grade operacional permanece disponível.

A chave do Google Maps é entregue ao navegador. Restrinja-a por referenciador HTTP aos domínios do CloudPort e limite sua utilização à Maps JavaScript API.

Em produção, `assets/configuracao.json` pode ser substituído sem reconstruir os artefatos estáticos.

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

Os testes unitários usam o test runner nativo do Node e cobrem sessão, papéis, erros, JWT, correlação, contratos operacionais, grade operacional, exportações, alertas, ajuda contextual, Gate, Rail, Yard e Vessel Planner.

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

## Implantação no EasyPanel

Use uma aplicação GitHub com:

| Campo | Valor |
| --- | --- |
| Repositório | `diogo2806/CloudPort` |
| Branch | `main` |
| Caminho de Build | `/frontend` |
| Construção | `Dockerfile` |
| Arquivo | `Dockerfile` |
| Porta | `80` |
| Health check | `/health` |

O `frontend/Dockerfile` usa Node 22 no estágio de build e Nginx no estágio final. O `frontend/nginx.conf` entrega os arquivos estáticos, expõe `/health` e usa fallback para `index.html` nas rotas da SPA.

## Regras para novos contratos e telas

- usar caminhos relativos à `baseApiUrl`;
- manter autenticação e `X-Correlation-Id` no cliente compartilhado;
- não reproduzir no frontend a decisão entre chamada local e remota do backend;
- consumir erros padronizados;
- preservar as rotas funcionais durante a migração do backend;
- não introduzir dependências Angular;
- reutilizar `OperationalDataGrid`, `PageHeader`, ajuda contextual e central de alertas antes de criar componentes paralelos;
- manter alternativa acessível para interações baseadas em drag-and-drop.
