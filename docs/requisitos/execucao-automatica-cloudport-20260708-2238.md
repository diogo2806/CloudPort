# Execucao automatica CloudPort - 2026-07-08 22:38

## Escopo

Repositorio: `diogo2806/CloudPort`.

Requisito analisado: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Objetivo desta rodada: transformar a pendencia ja identificada de work queues persistentes no Control Room em alteracao segura de back/front, atualizar o requisito e criar PR com merge apenas se a validacao fosse segura.

## Resultado da analise

O backend e o service Angular ja possuem o contrato necessario para listar work queues persistentes da visita:

- `GET /visitas-navio/{id}/integracao-patio/work-queues`
- Interface TypeScript `WorkQueuePatioDaVisita`
- Metodo `listarWorkQueuesPatio(visitaId)` em `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts`

A lacuna permanece no componente principal do frontend:

- `app.component.ts` ainda importa `FilaPatioDaVisita`, mas nao importa `WorkQueuePatioDaVisita`.
- O componente ainda possui `filasPatio`, mas nao possui estado `workQueuesPatio`.
- `carregarIntegracaoPatio()` ainda chama `listarFilasPatio(visitaId)`, mas nao chama `listarWorkQueuesPatio(visitaId)`.
- O HTML ainda renderiza apenas `Filas / POW operacionais` com `filasPatioFiltradas()`.
- Nao ha cards de work queue persistente com POW, pool, equipamento e job list expandivel.

## Patch funcional que deve ser aplicado no proximo corte de codigo

### `frontend/servico-navio-siderurgico/src/app/app.component.ts`

Adicionar no import vindo de `./siderurgico-api.service`:

```ts
  WorkQueuePatioDaVisita,
```

Adicionar o estado junto de `filasPatio`:

```ts
  workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

Adicionar filtro publico junto de `filasPatioFiltradas()`:

```ts
  workQueuesPatioFiltradas(): WorkQueuePatioDaVisita[] {
    return this.workQueuesPatio.filter(workQueue =>
      this.correspondeFiltroStatus(workQueue.status) &&
      this.correspondeFiltroBloco(`${workQueue.berco || ''} ${workQueue.blocoZona || ''} ${workQueue.pow || ''} ${workQueue.poolOperacional || ''} ${workQueue.equipamento || ''}`)
    );
  }
```

Limpar o estado quando nao houver visita:

```ts
      this.workQueuesPatio = [];
```

Carregar work queues apos `listarFilasPatio(visitaId)`:

```ts
    this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

Atualizar `substituirOrdemPatio()` para refletir atualizacoes nas job lists:

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

Quando o patch acima for aplicado e validado, remover ou marcar como entregue:

- Consumo no frontend de `GET /visitas-navio/{id}/integracao-patio/work-queues`.
- Estado `workQueuesPatio` no `AppComponent`.
- Renderizacao inicial de work queues persistentes no Control Room.
- Visualizacao expandivel da job list por work queue.

Continuam pendentes apos esse corte:

- Ativar/desativar work queue pela tela.
- Associar/editar POW, pool operacional e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacao executada

Validado por leitura via conector GitHub:

- `siderurgico-api.service.ts` possui `WorkQueuePatioDaVisita`.
- `siderurgico-api.service.ts` possui `listarWorkQueuesPatio(visitaId)`.
- `app.component.ts` ainda nao possui `workQueuesPatio`.
- `app.component.ts` ainda nao chama `listarWorkQueuesPatio(visitaId)`.
- `app.component.html` ainda nao renderiza cards de work queues persistentes.

Nao foi executado build Angular nem testes automatizados.

## Bloqueio para merge automatico

Nao foi seguro alterar diretamente `app.component.ts` e `app.component.html` porque a escrita disponivel pelo conector substitui o arquivo inteiro. Embora as leituras por intervalos tenham permitido confirmar o gap, aplicar uma substituicao integral manual desses arquivos sem clone local aumenta risco de perda de conteudo ou quebra de template.

O ambiente local tambem falhou ao clonar o repositorio por erro de resolucao de `github.com`, impedindo aplicar patch, rodar diff local, build e testes.

Por isso este PR deve permanecer aberto, sem merge automatico, como registro tecnico da validacao e do patch esperado.
