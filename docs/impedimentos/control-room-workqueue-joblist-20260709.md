# Impedimento - Control Room work queue job list

Data: 2026-07-09.

## Requisito analisado

Arquivo base: `docs/requisitos/modulo-navios-back-front-gaps.md`.

Pendencia alvo:

- Permitir expandir job list da work queue no template com work instructions, status, sequencia, prioridade, origem/destino e acoes disponiveis.

## Estado confirmado no codigo

- `frontend/servico-navio-siderurgico/src/app/siderurgico-api.service.ts` ja possui o contrato `WorkQueuePatioDaVisita` e o metodo `listarWorkQueuesPatio(visitaId)`.
- `frontend/servico-navio-siderurgico/src/app/app.component.ts` ja possui o estado `workQueuesPatio`, helpers de filtro e totalizacao, e carrega as work queues em `carregarIntegracaoPatio()`.
- `frontend/servico-navio-siderurgico/src/app/app.component.html` ja renderiza cards basicos das work queues persistentes.
- A job list detalhada ainda nao esta renderizada no HTML.

## Impedimento de implementacao segura nesta execucao

O conector GitHub disponivel para esta execucao permite substituir arquivos inteiros via Contents API, mas nao aplicar patch parcial localizado no meio de um arquivo grande.

O arquivo `frontend/servico-navio-siderurgico/src/app/app.component.html` possui centenas de linhas e concentra quase toda a tela. Sem clone local, sem build Angular e sem aplicacao de patch parcial, substituir o arquivo inteiro apenas para inserir o bloco da job list cria risco alto de regressao visual e estrutural na tela.

Por esse motivo, esta execucao nao alterou o template. A pendencia deve permanecer em `docs/requisitos/modulo-navios-back-front-gaps.md` ate ser aplicada por patch local ou por ambiente com clone/build.

## Patch recomendado

Inserir dentro do card de cada `workQueue` em `app.component.html`, logo apos a linha que exibe total de ordens e sequencia inicial:

```html
<details class="job-list-detail" *ngIf="workQueue.jobList?.length; else semJobListWorkQueue">
  <summary>Ver job list ({{ workQueue.jobList?.length || 0 }} ordem(ns))</summary>
  <div class="table-wrap">
    <table class="job-list-table">
      <thead>
        <tr>
          <th>Seq.</th>
          <th>Lote</th>
          <th>Mov.</th>
          <th>Origem</th>
          <th>Destino</th>
          <th>Prior.</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let ordem of workQueue.jobList">
          <td>{{ ordem.sequenciaNavio || '-' }}</td>
          <td>{{ ordem.codigoLote }}</td>
          <td>{{ ordem.tipoMovimento }}</td>
          <td>{{ ordem.origem || '-' }}</td>
          <td>{{ ordem.destino || ordem.posicaoPlanejada || '-' }}</td>
          <td>{{ ordem.prioridadeOperacional ?? '-' }}</td>
          <td><span class="badge" [ngClass]="ordem.statusOrdem">{{ ordem.statusOrdem }}</span></td>
        </tr>
      </tbody>
    </table>
  </div>
</details>
<ng-template #semJobListWorkQueue>
  <p class="muted">Job list detalhada nao retornada para esta fila.</p>
</ng-template>
```

Adicionar estilo em `app.component.css`:

```css
.job-list-detail {
  border-top: 1px solid #e2e8f0;
  margin-top: 12px;
  padding-top: 10px;
}

.job-list-detail summary {
  color: #1d4ed8;
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

.job-list-table {
  margin-top: 10px;
}
```

## Validacao necessaria antes de marcar como implementado

1. Build Angular do frontend.
2. Teste visual do Control Room com work queue contendo `jobList` preenchida.
3. Teste visual do Control Room com work queue sem `jobList` detalhada.
4. Confirmar que filtros de status e berco/bloco/zona continuam funcionando.
5. Somente apos isso, remover a pendencia de `docs/requisitos/modulo-navios-back-front-gaps.md` e registrar como implementado em `docs/implementados/requisitos-implementados.md`.
