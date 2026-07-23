# Visita de navio — marcos, janelas e plano de estiva

## Finalidade da tela

A tela permite consultar e manter os dados cadastrais e temporais de uma visita de navio, acompanhar itens operacionais e administrar o ciclo de validação e publicação do plano de estiva.

## Fluxo operacional

1. Selecione a visita e confirme navio, viagens, linha operadora, terminal e berço previsto.
2. Informe os marcos previstos ETA, ETB e ETD.
3. Configure a janela de recebimento e o cutoff operacional.
4. Registre a ATA quando a chegada efetiva for confirmada.
5. Avance a visita pelas transições operacionais. As transições registram automaticamente ATB, início da operação, fim da operação e ATD.
6. Mantenha os itens da operação, incluindo movimento, carga, quantidade, peso, porão, posição e sequência.
7. Valide o plano de estiva, trate erros e alertas e publique a versão aprovada.
8. Quando necessário, invalide ou cancele o plano com motivo e crie uma nova versão.

## Campos da visita

### Identificação

- **Código da visita:** identificador único da escala.
- **Navio:** embarcação vinculada à visita.
- **Viagem de entrada e saída:** códigos operacionais das viagens.
- **Linha operadora:** serviço ou linha informada para a escala.
- **Terminal/facility:** instalação responsável pela operação.
- **Berço previsto:** berço planejado.
- **Berço atual:** berço efetivamente utilizado.

### Marcos previstos

- **ETA:** chegada prevista.
- **ETB:** atracação prevista.
- **ETD:** partida prevista.

Esses campos são mantidos pelo planejamento e permanecem editáveis enquanto a visita não estiver encerrada.

### Marcos realizados

- **ATA:** chegada efetiva, informada após confirmação operacional.
- **ATB:** atracação efetiva, gravada ao transicionar para `ATRACADA`.
- **Início da operação:** gravado ao transicionar para `OPERANDO`.
- **Fim da operação:** gravado ao transicionar para `OPERACAO_CONCLUIDA`.
- **ATD:** partida efetiva, gravada ao transicionar para `PARTIU`.

Os marcos gerados por transição são somente leitura nesta tela para preservar a origem operacional e a auditoria.

### Recebimento

- **Início da janela de recebimento:** primeiro instante autorizado para recebimento associado à visita.
- **Fim da janela de recebimento:** último instante autorizado para recebimento.
- **Cutoff operacional:** limite operacional definido para aceite ou preparação da carga.

## Permissões necessárias

- A consulta depende de acesso ao módulo de Navio/Control Room.
- A alteração da visita exige permissão de planejamento ou administração definida no backend.
- Cancelamento, validação, publicação, invalidação e criação de nova versão dependem das permissões específicas da operação.
- A ocultação ou desabilitação de um botão no frontend não substitui a autorização do backend.

## Estados possíveis

- `PREVISTA`: visita planejada.
- `FUNDEADA`: navio aguardando atracação.
- `ATRACADA`: navio atracado; ATB registrado.
- `OPERANDO`: operação iniciada; início registrado.
- `OPERACAO_CONCLUIDA`: operação encerrada; fim registrado.
- `PARTIU`: navio partiu; ATD registrado e edição bloqueada.
- `CANCELADA`: visita cancelada e edição bloqueada.

## Motivos de bloqueio

- ATA anterior ao ETA.
- ETB anterior ao ETA.
- ATB anterior ao ETB.
- início da operação anterior ao ATB.
- fim da operação anterior ao início.
- ETD anterior ao ETA.
- ATD anterior ao ETD.
- fim da janela de recebimento anterior ao início.
- código da visita ou navio ausente.
- visita em `PARTIU` ou `CANCELADA`.
- item já operado ou cancelado.
- plano em estado incompatível com a ação solicitada.
- ausência da permissão exigida.

A tela mostra as inconsistências cronológicas antes do envio. O backend repete as mesmas validações e permanece como fonte final de autorização e consistência.

## Exemplo

Uma visita possui ETA às 08:00, ATA às 08:20, ETB às 09:00 e ETD às 19:00. Ao confirmar a atracação, a transição para `ATRACADA` grava o ATB. A transição para `OPERANDO` grava o início da operação. Após a conclusão, `OPERACAO_CONCLUIDA` grava o fim, e `PARTIU` grava o ATD. O operador não altera manualmente esses quatro marcos automáticos no editor.

## Atalhos

- `F1`: abre o manual contextual.
- `Shift + ?`: abre o manual quando o foco não está em um campo de edição.
- `Tab` e `Shift + Tab`: percorrem campos e ações.
- `Enter`: envia o formulário ativo quando a ação estiver habilitada.
- `Esc`: utilize o botão Fechar para encerrar o manual sem perder o conteúdo editado.

## Processo completo

O processo completo combina cadastro da visita, transições de fase, manutenção dos itens, validação do plano de estiva, resolução de bloqueios, publicação e acompanhamento do histórico de eventos. As transições devem ser executadas pelas ações operacionais específicas para que horários reais e auditoria permaneçam consistentes.