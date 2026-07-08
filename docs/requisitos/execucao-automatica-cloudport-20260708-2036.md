# Execucao automatica de requisitos CloudPort - 2026-07-08 20:36

## Escopo analisado

Repositorio: `diogo2806/CloudPort`.

Documento principal encontrado: `docs/requisitos/modulo-navios-back-front-gaps.md`.

A analise desta execucao focou nas pendencias de integracao Back x Front do modulo Navio + Patio, principalmente o trecho que indica que o backend e o service Angular ja possuem contrato para work queues, mas o `AppComponent` ainda nao mantem estado nem renderiza `workQueuesPatio`.

## Evidencia encontrada no requisito

O requisito atual registra que ja existem:

- `GET /visitas-navio/{id}/integracao-patio/work-queues` no modulo de navio.
- Tipo frontend `WorkQueuePatioDaVisita`.
- Metodo `listarWorkQueuesPatio(visitaId)` no `SiderurgicoApiService`.

Tambem registra como pendencia:

- Importar `WorkQueuePatioDaVisita` no `AppComponent`.
- Criar estado `workQueuesPatio`.
- Chamar `listarWorkQueuesPatio()` dentro de `carregarIntegracaoPatio()`.
- Renderizar cards de work queue persistente no Control Room.

## Verificacao no codigo

Arquivos inspecionados:

- `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts`
- `frontend/servico-navio-siderurgico/src/app/app.component.ts`
- `frontend/servico-navio-siderurgico/src/app/app.component.html`

Resultado:

1. O service Angular ja contem `WorkQueuePatioDaVisita` e `listarWorkQueuesPatio(visitaId)`.
2. O componente ainda usa `FilaPatioDaVisita[]` como visao operacional principal.
3. O componente ainda nao possui estado `workQueuesPatio`.
4. O carregamento de integracao ainda nao busca `/integracao-patio/work-queues`.
5. O HTML ainda renderiza somente a tabela antiga de `filasPatio`, sem cards de work queue persistente com POW, pool, equipamento e job list.

## Implementacao indicada para o proximo corte de codigo

A alteracao de codigo segura deve ser feita nos seguintes pontos:

### `app.component.ts`

1. Adicionar `WorkQueuePatioDaVisita` no import vindo de `./siderurgico-api.service`.
2. Declarar estado:

```ts
workQueuesPatio: WorkQueuePatioDaVisita[] = [];
```

3. Limpar o estado quando nao houver visita selecionada:

```ts
this.workQueuesPatio = [];
```

4. Buscar as work queues durante o carregamento da integracao:

```ts
this.workQueuesPatio = await this.api.listarWorkQueuesPatio(visitaId);
```

5. Criar filtro especifico para work queues:

```ts
workQueuesPatioFiltradas(): WorkQueuePatioDaVisita[] {
  return this.workQueuesPatio.filter(workQueue =>
    this.correspondeFiltroStatus(workQueue.status) &&
    this.correspondeFiltroBloco(`${workQueue.berco || ''} ${workQueue.blocoZona || ''} ${workQueue.pow || ''} ${workQueue.poolOperacional || ''} ${workQueue.equipamento || ''}`)
  );
}
```

### `app.component.html`

Adicionar uma secao no Control Room antes da tabela antiga de filas:

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
    </details>
  </article>
</section>
<ng-template #semWorkQueuesPatio><p>Nenhuma work queue persistente retornada para a visita.</p></ng-template>
```

### `app.component.css`

Adicionar estilos simples para os cards:

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
```

## Atualizacao do requisito recomendada

Quando o corte de codigo acima for aplicado, remover ou marcar como entregue estes itens:

- A.1: consumo completo do contrato `GET /visitas-navio/{id}/integracao-patio/work-queues` no front.
- C.2: estado e renderizacao inicial de `workQueuesPatio` com cards por fila persistente.
- Parte inicial de C.3: exibicao expandivel da job list retornada dentro da work queue.

Devem permanecer pendentes:

- Acoes de ativar/desativar work queue.
- Associacao de POW, pool e equipamento pela tela.
- Dispatch pelo frontend.
- Reset/cancelamento de work instruction.
- Historico e drill-down completo da work instruction.
- SSE/WebSocket substituindo polling.
- Contratos OpenAPI e testes de contrato.

## Validacao desta execucao

Nao foi possivel executar build local nesta rodada porque o ambiente nao conseguiu resolver `github.com` para clonar o repositorio. A validacao feita foi por leitura direta dos arquivos pelo conector GitHub.

Por esse motivo, este PR deve ser tratado como uma atualizacao documental/tecnica de backlog, sem merge automatico caso existam checks obrigatorios, revisao obrigatoria ou conflito.
