# Execucao automatica CloudPort - 2026-07-08 21:39

## Escopo

Repositorio: `diogo2806/CloudPort`.

Requisito analisado: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Objetivo desta rodada: implementar a pendencia de integracao Back x Front relacionada ao consumo de work queues persistentes no Control Room do frontend `servico-navio-siderurgico`.

## Evidencia confirmada no codigo atual

Foram inspecionados os arquivos:

- `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts`
- `frontend/servico-navio-siderurgico/src/app/app.component.ts`
- `frontend/servico-navio-siderurgico/src/app/app.component.html`
- `frontend/servico-navio-siderurgico/src/app/app.component.css`
- `docs/requisitos/modulo-navios-back-front-gaps.md`

Confirmacoes:

1. O service Angular ja possui o contrato `WorkQueuePatioDaVisita`.
2. O service Angular ja possui o metodo `listarWorkQueuesPatio(visitaId)`, apontando para `GET /visitas-navio/{id}/integracao-patio/work-queues`.
3. O requisito principal ainda registra que o `AppComponent` nao importa `WorkQueuePatioDaVisita`, nao mantem estado `workQueuesPatio`, nao chama `listarWorkQueuesPatio()` em `carregarIntegracaoPatio()` e nao renderiza cards de work queue persistente.
4. O `app.component.ts` atual possui estado `filasPatio`, mas nao possui estado `workQueuesPatio`.
5. O `carregarIntegracaoPatio()` atual carrega resumo, reservas, ordens, filas antigas, ordens sem cobertura e alertas, mas nao carrega work queues persistentes.
6. O HTML atual renderiza `Filas / POW operacionais` usando `filasPatioFiltradas()`, mas nao renderiza POW, pool, equipamento e job list retornados por `WorkQueuePatioDaVisita`.

## Implementacao que deve ser aplicada no codigo

### 1. `app.component.ts`

Adicionar `WorkQueuePatioDaVisita` no import vindo de `./siderurgico-api.service`:

```ts
  WorkQueuePatioDaVisita
```

Declarar o estado ao lado de `filasPatio`:

```ts
  workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

Criar o filtro publico do componente, ao lado de `filasPatioFiltradas()`:

```ts
  workQueuesPatioFiltradas(): WorkQueuePatioDaVisita[] {
    return this.workQueuesPatio.filter(workQueue =>
      this.correspondeFiltroStatus(workQueue.status) &&
      this.correspondeFiltroBloco(`${workQueue.berco || ''} ${workQueue.blocoZona || ''} ${workQueue.pow || ''} ${workQueue.poolOperacional || ''} ${workQueue.equipamento || ''}`)
    );
  }
```

Limpar o estado quando nao houver visita selecionada, dentro de `carregarIntegracaoPatio()`:

```ts
      this.workQueuesPatio = [];
```

Carregar as work queues apos `listarFilasPatio(visitaId)`:

```ts
    this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

Atualizar `substituirOrdemPatio()` para refletir alteracoes tambem nas job lists das work queues:

```ts
    this.workQueuesPatio = this.workQueuesPatio.map(workQueue => ({
      ...workQueue,
      jobList: workQueue.jobList.map(ordem => ordem.id === ordemAtualizada.id ? ordemAtualizada : ordem)
    }));
```

### 2. `app.component.html`

Inserir uma secao antes da tabela antiga `Filas / POW operacionais`:

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

### 3. `app.component.css`

Adicionar os estilos:

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

## Atualizacao esperada do requisito depois da implementacao

Quando o codigo acima estiver aplicado e validado, marcar como entregue/remover da lista de pendencias:

- Consumo frontend de `GET /visitas-navio/{id}/integracao-patio/work-queues`.
- Estado `workQueuesPatio` no `AppComponent`.
- Renderizacao inicial de cards de work queue persistente no Control Room.
- Visualizacao expandivel da job list retornada dentro da work queue.

Devem continuar pendentes:

- Acao de ativar/desativar work queue pela tela.
- Associacao/edicao de POW, pool e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional da work instruction.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Bloqueio desta execucao

A alteracao direta nos arquivos Angular nao foi aplicada porque o conector disponivel para escrita substitui arquivos inteiros via Contents API. O conteudo completo dos arquivos `app.component.ts` e `app.component.html` foi retornado de forma truncada nas leituras, e o ambiente local nao conseguiu clonar o repositorio por falha de resolucao de `github.com`.

Para evitar sobrescrever partes nao lidas do componente e quebrar o frontend, este PR deve permanecer aberto como registro tecnico de bloqueio, sem merge automatico.

## Validacao executada

Validacao por leitura via conector GitHub:

- Contrato `WorkQueuePatioDaVisita` encontrado no service.
- Metodo `listarWorkQueuesPatio(visitaId)` encontrado no service.
- Ausencia de `workQueuesPatio` confirmada no componente.
- Ausencia de renderizacao de cards de work queue persistente confirmada no HTML.

Validacao nao executada:

- Build Angular.
- Testes automatizados.
- Aplicacao do patch de codigo.

Motivo: impossibilidade de clonar o repositorio no ambiente local e risco de sobrescrita parcial ao atualizar arquivos truncados.
