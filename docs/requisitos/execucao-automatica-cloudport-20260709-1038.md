# Execucao automatica CloudPort - 2026-07-09 10:38 BRT

## Escopo

Repositorio: `diogo2806/CloudPort`.

Objetivo da execucao: analisar `docs/requisitos`, implementar pendencias back-end/front-end quando seguro, atualizar requisito, criar PR e fazer merge quando apto.

## Requisito analisado

Arquivo principal: `docs/requisitos/modulo-navios-back-front-gaps.md`.

A pendencia mais objetiva e recorrente permanece no front-end Angular do modulo `frontend/servico-navio-siderurgico`: consumir e renderizar work queues persistentes no Control Room.

## Evidencia tecnica encontrada

### Ja implementado no back/service

O arquivo `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` ja possui:

- Interface `WorkQueuePatioDaVisita`.
- Metodo `listarWorkQueuesPatio(visitaId)`.
- Contrato front-end para `GET /visitas-navio/{id}/integracao-patio/work-queues`.

Contrato confirmado no service:

```ts
export interface WorkQueuePatioDaVisita {
  id?: number | null;
  identificador: string;
  agrupamento: string;
  visitaNavioId: number;
  berco?: string | null;
  porao?: number | null;
  blocoZona?: string | null;
  sequenciaInicial?: number | null;
  pow?: string | null;
  poolOperacional?: string | null;
  equipamento?: string | null;
  status: string;
  prioridadeOperacional?: number | null;
  totalOrdens: number;
  jobList: OrdemPatioDaVisita[];
  criadoEm?: string | null;
  atualizadoEm?: string | null;
}

listarWorkQueuesPatio(visitaId: number): Promise<WorkQueuePatioDaVisita[]> {
  return firstValueFrom(this.http.get<WorkQueuePatioDaVisita[]>(`${this.baseApiUrl}/visitas-navio/${visitaId}/integracao-patio/work-queues`).pipe(timeout(5000)));
}
```

### Ainda pendente no front

No `frontend/servico-navio-siderurgico/src/app/app.component.ts`:

- O import ainda traz `FilaPatioDaVisita`, mas nao traz `WorkQueuePatioDaVisita`.
- Existe `filasPatio: FilaPatioDaVisita[] = [];`, mas nao existe `workQueuesPatio: WorkQueuePatioDaVisita[] = [];`.
- `carregarIntegracaoPatio()` chama `listarFilasPatio(visitaId)`, mas ainda nao chama `listarWorkQueuesPatio(visitaId)`.
- `substituirOrdemPatio()` atualiza `filasPatio.ordens`, mas nao atualiza `workQueuesPatio.jobList`.

No `frontend/servico-navio-siderurgico/src/app/app.component.html`:

- O Control Room ainda renderiza `Filas / POW operacionais` com `filasPatioFiltradas()`.
- Ainda nao ha cards persistentes de work queue com POW, pool, equipamento e job list expandivel.

No `frontend/servico-navio-siderurgico/src/app/app.component.css`:

- Ainda nao existem estilos dedicados a `.work-queue-grid` e `.work-queue-card`.

## Patch funcional recomendado

### `app.component.ts`

Adicionar ao import:

```ts
  WorkQueuePatioDaVisita,
```

Adicionar estado junto de `filasPatio`:

```ts
  workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

Adicionar metodo de filtro:

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

### `app.component.html`

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

### `app.component.css`

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

## Por que a alteracao funcional nao foi gravada nesta execucao

O conector GitHub disponivel para escrita substitui arquivos inteiros via `update_file`. Os arquivos Angular envolvidos sao grandes e a leitura dos arquivos completos e truncada nas respostas do conector. Como nao ha clone local, diff local nem build Angular disponivel neste ambiente, substituir `app.component.ts`, `app.component.html` e `app.component.css` integralmente criaria risco real de regressao por perda acidental de trechos nao visiveis.

Por esse motivo, esta execucao nao marca a pendencia como desenvolvida. O requisito deve continuar considerando pendente:

- Consumo real de `GET /visitas-navio/{id}/integracao-patio/work-queues` no `AppComponent`.
- Estado `workQueuesPatio` no componente.
- Cards persistentes de work queue no Control Room.
- Job list expandivel por work queue.
- Sincronizacao local de `workQueuesPatio.jobList` apos atualizar prioridade/suspensao/retomada de ordem.

## Base de conhecimento / requisitos complementares ainda faltantes

Continuam faltando no sistema:

- Tela para ativar/desativar work queue.
- Tela para associar/editar POW, pool operacional e equipamento.
- Acao de dispatch de work queue no frontend.
- Reset/cancelamento de work instruction pelo frontend.
- CHE detail panel real.
- Job list por equipamento/pool.
- Drill-down completo de historico operacional.
- SSE/WebSocket para substituir polling de Control Room.
- Contratos OpenAPI formais e testes de contrato.
- Paginacao, filtros, `fields`, `correlationId`, autenticacao por app/client e contrato de erro padronizado para `/api/public/v1`.
- Fluxos EDI/SFTP para BAPLIE, COPRAR, COARRI e eventos operacionais.

## Validacao possivel nesta execucao

Validado por leitura via conector GitHub:

- O repositorio esta acessivel com permissao administrativa.
- O requisito principal existe e ainda lista o gap de work queues no front.
- O service Angular possui `WorkQueuePatioDaVisita` e `listarWorkQueuesPatio`.
- O `AppComponent` ainda nao possui estado/renderizacao de `workQueuesPatio`.
- O HTML do Control Room ainda nao possui cards persistentes de work queue.

Nao foi possivel executar build/testes porque nao ha clone local funcional nem acesso a engine de build do projeto neste ambiente.
