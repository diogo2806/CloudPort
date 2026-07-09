# Execucao automatica CloudPort - 2026-07-09 06:30

## Escopo

Repositorio solicitado: `diogo2806/claudeport`.

Repositorio encontrado no conector: `diogo2806/CloudPort`. O nome `diogo2806/claudeport` retornou 404 no conector GitHub, e a busca por `claudeport` nao retornou repositorio instalado. Por isso a analise foi executada em `diogo2806/CloudPort`.

Requisito analisado: `docs/requisitos/modulo-navios-back-front-gaps.md` e requisitos de execucao automatica mais recentes em `docs/requisitos`.

## Resultado da analise back/front

O requisito principal ja declara que o backend possui contratos de work queues no `servico-yard`, proxy no modulo de navio e contrato publico `/api/public/v1`. Tambem declara que o service Angular possui `WorkQueuePatioDaVisita` e `listarWorkQueuesPatio(visitaId)`.

A leitura do codigo confirmou:

- `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` possui a interface `WorkQueuePatioDaVisita`.
- `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` possui `listarWorkQueuesPatio(visitaId)` chamando `GET /visitas-navio/{id}/integracao-patio/work-queues`.
- `frontend/servico-navio-siderurgico/src/app/app.component.ts` ainda importa `FilaPatioDaVisita`, mas nao importa `WorkQueuePatioDaVisita`.
- `AppComponent` ainda possui `filasPatio`, mas nao possui `workQueuesPatio`.
- `carregarIntegracaoPatio()` ainda chama `listarFilasPatio(visitaId)`, mas nao chama `listarWorkQueuesPatio(visitaId)`.
- `app.component.html` ainda renderiza `Filas / POW operacionais`, mas nao renderiza cards de work queues persistentes com POW, pool, equipamento e job list expandivel.

## Alteracao funcional que deve ser aplicada

### `frontend/servico-navio-siderurgico/src/app/app.component.ts`

Adicionar `WorkQueuePatioDaVisita` ao import vindo de `./siderurgico-api.service`.

Adicionar estado:

```ts
workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

Adicionar filtro publico:

```ts
workQueuesPatioFiltradas(): WorkQueuePatioDaVisita[] {
  return this.workQueuesPatio.filter(workQueue =>
    this.correspondeFiltroStatus(workQueue.status) &&
    this.correspondeFiltroBloco(`${workQueue.berco || ''} ${workQueue.blocoZona || ''} ${workQueue.pow || ''} ${workQueue.poolOperacional || ''} ${workQueue.equipamento || ''}`)
  );
}
```

Limpar estado quando nao houver visita:

```ts
this.workQueuesPatio = [];
```

Carregar junto da integracao de patio:

```ts
this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

Atualizar `substituirOrdemPatio()` para refletir alteracoes dentro das job lists:

```ts
this.workQueuesPatio = this.workQueuesPatio.map(workQueue => ({
  ...workQueue,
  jobList: workQueue.jobList.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem)
}));
```

### `frontend/servico-navio-siderurgico/src/app/app.component.html`

Inserir antes da tabela antiga `Filas / POW operacionais`:

```html
<section class="work-queue-grid" *ngIf="workQueuesPatioFiltradas().length; else semWorkQueuesPatio">
  <article class="work-queue-card" *ngFor="let workQueue of workQueuesPatioFiltradas()">
    <header>
      <strong>{{ workQueue.identificador }}</strong>
      <span class="badge" [ngClass]="workQueue.status">{{ workQueue.status }}</span>
    </header>
    <p>Berco: {{ workQueue.berco || '-' }} · Porao: {{ workQueue.porao || '-' }} · Bloco/zona: {{ workQueue.blocoZona || '-' }}</p>
    <p>POW: {{ workQueue.pow || '-' }} · Pool: {{ workQueue.poolOperacional || '-' }} · Equipamento: {{ workQueue.equipamento || '-' }}</p>
    <p>Prioridade: {{ workQueue.prioridadeOperacional ?? '-' }} · Ordens: {{ workQueue.totalOrdens }}</p>
    <details>
      <summary>Job list ({{ workQueue.jobList.length }})</summary>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Seq.</th><th>Lote</th><th>Mov.</th><th>Origem</th><th>Destino</th><th>Status</th></tr></thead>
          <tbody>
            <tr *ngFor="let ordem of workQueue.jobList">
              <td>{{ ordem.sequenciaNavio || '-' }}</td>
              <td>{{ ordem.codigoLote }}</td>
              <td>{{ ordem.tipoMovimento }}</td>
              <td>{{ ordem.origem || '-' }}</td>
              <td>{{ ordem.destino || ordem.posicaoPlanejada || '-' }}</td>
              <td><span class="badge" [ngClass]="ordem.statusOrdem">{{ ordem.statusOrdem }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </details>
  </article>
</section>
<ng-template #semWorkQueuesPatio><p>Nenhuma work queue persistente retornada para a visita.</p></ng-template>
```

### `frontend/servico-navio-siderurgico/src/app/app.component.css`

Adicionar estilos para `.work-queue-grid` e `.work-queue-card`, com grid responsivo, card com borda, header flexivel e `details/summary` legiveis.

## Atualizacao do requisito

O item abaixo continua pendente no requisito principal enquanto o patch funcional nao for aplicado diretamente aos arquivos Angular:

- Consumo e renderizacao no frontend de `GET /visitas-navio/{id}/integracao-patio/work-queues`.

Quando aplicado, remover da secao de pendencias ou marcar como entregue:

- Import de `WorkQueuePatioDaVisita` no `AppComponent`.
- Estado `workQueuesPatio`.
- Chamada a `listarWorkQueuesPatio(visitaId)` em `carregarIntegracaoPatio()`.
- Cards de work queues persistentes no Control Room.
- Job list expandivel por work queue.

Continuam pendentes mesmo apos esse corte:

- Ativar/desativar work queue pela tela.
- Associar/editar POW, pool operacional e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacoes executadas

Validacao por leitura via conector GitHub:

- Confirmado contrato `WorkQueuePatioDaVisita` no service Angular.
- Confirmado metodo `listarWorkQueuesPatio(visitaId)` no service Angular.
- Confirmado gap em `AppComponent` e no template do Control Room.

Nao foi possivel executar build Angular nem testes locais porque o ambiente local falhou ao clonar o repositorio por erro de resolucao de `github.com`.

## Bloqueio para merge automatico

Nao foi seguro substituir integralmente `app.component.ts`, `app.component.html` e `app.component.css` via conector porque a acao disponivel de escrita substitui o arquivo inteiro. Sem clone local e sem validacao automatizada, a substituicao manual integral desses arquivos aumenta risco de perda de conteudo ou quebra de template.

Por isso este PR deve permanecer aberto, sem merge automatico, ate que o patch seja aplicado em ambiente com clone local, diff completo e build/testes executaveis.
