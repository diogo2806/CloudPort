# Execucao automatica CloudPort - 2026-07-09 - Work queues no Control Room

## Escopo

Repositorio: `diogo2806/CloudPort`.

Requisito principal analisado: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Objetivo solicitado: implementar pendencias de back-end/front-end, atualizar requisitos, criar PR, validar e fazer merge somente se seguro.

## Resultado da analise

O conector confirmou que o repositorio esta acessivel com permissao de administracao. Tambem foram consultados PRs recentes e arquivos do modulo Angular.

A pendencia funcional principal continua sendo a integracao visual de work queues persistentes no Control Room do frontend `frontend/servico-navio-siderurgico`.

O service Angular ja contem:

- Interface `WorkQueuePatioDaVisita`.
- Metodo `listarWorkQueuesPatio(visitaId)`.
- Contrato `GET /visitas-navio/{id}/integracao-patio/work-queues`.

O componente atual ainda precisa consumir e renderizar esse contrato:

- `app.component.ts` importa `FilaPatioDaVisita`, mas ainda nao importa `WorkQueuePatioDaVisita`.
- O componente possui `filasPatio`, mas ainda nao possui `workQueuesPatio`.
- `carregarIntegracaoPatio()` ainda carrega `listarFilasPatio(visitaId)`, mas ainda nao carrega `listarWorkQueuesPatio(visitaId)`.
- `app.component.html` ainda mostra a tabela antiga `Filas / POW operacionais`, sem cards persistentes de work queue com POW, pool, equipamento e job list expandivel.
- `substituirOrdemPatio()` ainda sincroniza `ordensPatio`, `ordensSemCobertura` e `filasPatio`, mas nao sincroniza `workQueuesPatio.jobList`.

## Alteracao funcional que deve ser aplicada

### `frontend/servico-navio-siderurgico/src/app/app.component.ts`

Adicionar ao import vindo de `./siderurgico-api.service`:

```ts
  WorkQueuePatioDaVisita,
```

Adicionar estado junto de `filasPatio`:

```ts
  workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

Adicionar metodo de filtro junto de `filasPatioFiltradas()`:

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

Carregar work queues em `carregarIntegracaoPatio()` apos `listarFilasPatio(visitaId)`:

```ts
    this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

Atualizar `substituirOrdemPatio()`:

```ts
    this.workQueuesPatio = this.workQueuesPatio.map(workQueue => ({
      ...workQueue,
      jobList: workQueue.jobList.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem)
    }));
```

### `frontend/servico-navio-siderurgico/src/app/app.component.html`

Inserir antes da tabela `Filas / POW operacionais`:

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

Adicionar:

```css
.work-queue-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.work-queue-card {
  border: 1px solid #d7dde8;
  border-radius: 0.75rem;
  padding: 1rem;
  background: #fff;
}

.work-queue-card header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
}

.work-queue-card p {
  margin: 0.5rem 0;
}

.work-queue-card details {
  margin-top: 0.75rem;
}

.work-queue-card summary {
  cursor: pointer;
  font-weight: 800;
}
```

## Atualizacao do requisito

Nao foi correto marcar a pendencia como desenvolvida nesta execucao porque o patch funcional nao foi gravado nos arquivos Angular. O requisito principal deve continuar mantendo como pendente:

- Consumo real de `GET /visitas-navio/{id}/integracao-patio/work-queues` no `AppComponent`.
- Estado `workQueuesPatio`.
- Renderizacao de cards de work queue persistente no Control Room.
- Job list expandivel por work queue.

Devem permanecer tambem:

- Ativar/desativar work queue pela tela.
- Associar/editar POW, pool operacional e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacao executada

Validado por leitura via conector GitHub:

- O repositorio `diogo2806/CloudPort` existe e esta acessivel.
- O service Angular contem `WorkQueuePatioDaVisita`.
- O service Angular contem `listarWorkQueuesPatio(visitaId)`.
- O `AppComponent` ainda nao possui estado `workQueuesPatio`.
- O HTML do Control Room ainda nao possui os cards persistentes de work queue.

Nao foi possivel executar build/testes localmente porque o ambiente de execucao nao consegue resolver `github.com` para clonar o repositorio.

## Bloqueio

A escrita disponivel pelo conector substitui arquivos inteiros. Como os arquivos Angular sao grandes e as respostas de leitura sao truncadas, nao foi seguro substituir `app.component.ts`, `app.component.html` e `app.component.css` integralmente sem clone local, diff completo e build/testes.

Este registro foi criado para manter rastreabilidade da analise. Nao deve ser considerado entrega funcional de codigo.
