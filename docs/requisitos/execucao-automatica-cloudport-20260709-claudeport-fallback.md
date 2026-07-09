# Execucao automatica CloudPort - 2026-07-09

## Escopo solicitado

Repositorio solicitado pelo usuario: `diogo2806/claudeport`.

Resultado da resolucao do repositorio: o conector GitHub retornou `Not Found` para `diogo2806/claudeport`. Entre os repositorios acessiveis foi encontrada a melhor correspondencia nominal `diogo2806/CloudPort`, usada nesta execucao como fallback operacional.

## Arquivos de requisito analisados

- `docs/requisitos/modulo-navios-back-front-gaps.md`
- `docs/requisitos/execucao-automatica-cloudport-20260708-2036.md`
- `docs/requisitos/execucao-automatica-cloudport-20260708-2238.md`

## Situacao encontrada

O requisito principal ja registra que o backend e o service Angular possuem o contrato para work queues persistentes:

- `GET /visitas-navio/{id}/integracao-patio/work-queues`
- Tipo frontend `WorkQueuePatioDaVisita`
- Metodo `listarWorkQueuesPatio(visitaId)` no `SiderurgicoApiService`

A lacuna de integracao Back x Front permanece no componente principal do frontend:

- `frontend/servico-navio-siderurgico/src/app/app.component.ts` ainda importa `FilaPatioDaVisita`, mas nao importa `WorkQueuePatioDaVisita`.
- O componente ainda possui `filasPatio`, mas nao possui estado `workQueuesPatio`.
- `carregarIntegracaoPatio()` ainda chama `listarFilasPatio(visitaId)`, mas nao chama `listarWorkQueuesPatio(visitaId)`.
- `substituirOrdemPatio()` ainda atualiza `ordensPatio`, `ordensSemCobertura` e `filasPatio`, mas nao propaga alteracoes para `workQueuesPatio.jobList`.
- `frontend/servico-navio-siderurgico/src/app/app.component.html` ainda renderiza a tabela antiga de `Filas / POW operacionais`, sem cards de work queue persistente com POW, pool, equipamento e job list expandivel.

## Patch funcional que continua pendente

### `frontend/servico-navio-siderurgico/src/app/app.component.ts`

Adicionar o tipo no import do service:

```ts
  WorkQueuePatioDaVisita,
```

Adicionar estado junto de `filasPatio`:

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

Limpar estado quando nao houver visita:

```ts
      this.workQueuesPatio = [];
```

Carregar work queues em `carregarIntegracaoPatio()` apos `listarFilasPatio(visitaId)`:

```ts
    this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

Atualizar `substituirOrdemPatio()` para refletir atualizacoes na job list:

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

Adicionar estilos:

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

Nao foi correto marcar a pendencia como entregue nesta execucao, porque o patch de codigo nao foi aplicado com seguranca.

Continuam pendentes no requisito:

- Consumo real no componente Angular de `GET /visitas-navio/{id}/integracao-patio/work-queues`.
- Estado `workQueuesPatio` no `AppComponent`.
- Cards de work queue persistente no Control Room.
- Job list expandivel por work queue persistente.
- Ativar/desativar work queue pela tela.
- Associar/editar POW, pool operacional e equipamento pela tela.
- Dispatch de work queue pelo frontend.
- Reset/cancelamento de work instruction pela tela.
- Drill-down completo de historico operacional.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacoes executadas

Validacao por leitura via conector GitHub:

- O repositorio `diogo2806/claudeport` nao esta acessivel ou nao existe com esse nome.
- O repositorio `diogo2806/CloudPort` existe e foi usado como fallback.
- `docs/requisitos/modulo-navios-back-front-gaps.md` contem a pendencia de work queues no frontend.
- `app.component.ts` ainda nao possui `workQueuesPatio` nem chamada a `listarWorkQueuesPatio(visitaId)`.
- `app.component.html` ainda nao possui cards de work queue persistente.

Nao foi possivel executar build Angular nem testes automatizados porque o clone local falhou por erro de resolucao de `github.com` no ambiente de execucao.

## Bloqueio para merge automatico

Este PR deve permanecer aberto, sem merge automatico, pelos seguintes motivos:

1. O repositorio solicitado literalmente (`diogo2806/claudeport`) nao foi encontrado; a execucao usou `diogo2806/CloudPort` por inferencia.
2. O patch funcional exige substituicao segura de arquivos grandes (`app.component.ts`, `app.component.html`, `app.component.css`), mas o ambiente local nao conseguiu clonar o repositorio para aplicar diff e rodar validacoes.
3. Sem build/testes, nao e seguro afirmar que a alteracao compila.
4. O PR documenta o bloqueio e o patch exato para aplicacao segura em uma execucao com clone/build disponiveis.
