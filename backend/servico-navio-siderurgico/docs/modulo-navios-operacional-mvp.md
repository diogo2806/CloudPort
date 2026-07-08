# Modulo de navios siderurgicos - MVP operacional

Este documento registra o primeiro corte de evolucao do modulo de navios siderurgicos para reduzir o gap entre o cadastro basico atual e um fluxo operacional minimo.

## Entrega deste corte

- A tela principal Angular deixou de depender de iframe para a operacao principal.
- A tela passou a consumir APIs reais do backend existente para navios, operacoes siderurgicas e itens de carga.
- A interface permite cadastrar navio, cadastrar operacao/visita, selecionar uma operacao e cadastrar/listar itens de carga/descarga.
- Foram adicionados indicadores visuais de quantidade de navios, quantidade de operacoes, peso total da operacao selecionada, progresso por itens operados e itens bloqueados.
- Os assets HTML de estiva continuam acessiveis como legado, mas nao sao mais o fluxo principal da tela.

## APIs consumidas pelo frontend

- `GET /navios-siderurgicos`
- `POST /navios-siderurgicos`
- `GET /operacoes-siderurgicas?navioId=`
- `POST /operacoes-siderurgicas`
- `GET /operacoes-siderurgicas/{operacaoId}/itens`
- `POST /operacoes-siderurgicas/{operacaoId}/itens`

## Proximo corte recomendado

1. Criar entidade unificada `VisitaNavio` para substituir a separacao entre escala simples e operacao siderurgica.
2. Criar entidades de manifesto operacional, plano de estiva, posicoes de estiva e eventos.
3. Incluir validacoes de duplicidade de lote por visita/movimento e duplicidade de posicao ativa no plano.
4. Expor resumo operacional calculado pelo backend.
5. Separar a tela atual em componentes especificos de lista de visitas, detalhe da visita, carga/descarga, plano de estiva, eventos e bloqueios.
