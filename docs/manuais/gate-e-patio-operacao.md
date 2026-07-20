# Manual operacional — Gate e pátio

Este manual descreve o processo operacional esperado para as capacidades BUS1860 a BUS1910. As funcionalidades permanecem pendentes enquanto não houver implementação completa no backend, frontend, persistência, autorização e auditoria.

## 1. Finalidade

Centralizar o tratamento seguro e auditável de exceções do pátio e do Gate: work instructions inconsistentes, zonas temporárias de segurança, avisos de estivagem, regras de appointments, exchange areas e pesagens.

## 2. Fluxo operacional completo

1. O operador identifica a visita, unidade, work instruction, posição, equipamento ou appointment.
2. O sistema valida inventário, destino, sequência, equipamento, restrições, capacidade, regras de acesso e permissões.
3. Quando houver inconsistência, o fluxo normal é bloqueado e um caso operacional persistido é aberto.
4. O responsável analisa motivo, severidade, snapshot, dependências e ação recomendada.
5. A correção é registrada com usuário, data, comando idempotente e evidências.
6. O sistema revalida a condição original.
7. Somente após revalidação positiva o item retorna ao fluxo normal.
8. Cancelamentos, substituições, overrides e liberações preservam histórico e compensações.

## 3. BUS1860 — Purgatório de work instructions

### Finalidade da tela

Separar instruções inconsistentes da fila normal de dispatch e permitir triagem, correção, revalidação, reentrada, substituição ou cancelamento compensado.

### Campos

- Identificador da work instruction e da ordem de trabalho.
- Motivo da quarentena.
- Origem da detecção.
- Severidade.
- Snapshot do inventário, posição, equipamento, sequência e dependências.
- Responsável atual.
- Ação recomendada.
- Estado da resolução.
- Histórico de comandos e revalidações.

### Permissões

- `ADMIN_PORTO`: consulta, atribuição, resolução, substituição e cancelamento.
- `PLANEJADOR`: análise, correção de planejamento e reencaminhamento.
- `OPERADOR_PATIO`: consulta, registro de evidências e execução das ações autorizadas.

### Estados possíveis

`ABERTO`, `EM_ANALISE`, `AGUARDANDO_CORRECAO`, `REVALIDANDO`, `LIBERADO`, `SUBSTITUIDO`, `CANCELADO`.

### Motivos de bloqueio

Destino inválido, conflito de inventário, posição indisponível, equipamento incompatível, sequência inválida, dependência não concluída, zona de segurança ativa ou caso já encerrado.

### Exemplo

Uma instrução de remoção aponta para uma posição já ocupada. O sistema retira a instrução do dispatch, registra o snapshot, atribui o caso ao planejador e somente a reencaminha após a definição de um destino válido e nova validação.

## 4. BUS1870 — Zonas temporárias de segurança

### Finalidade da tela

Criar, ativar, prorrogar e liberar áreas temporárias onde existem pessoas trabalhando no pátio.

### Campos

- Nome e identificador da zona.
- Geometria poligonal ou posições afetadas.
- Início e fim da vigência.
- Responsável e equipe.
- Motivo.
- Estado.
- Movimentos, rotas e allocations conflitantes.

### Permissões

- `ADMIN_PORTO`: todas as ações.
- `PLANEJADOR`: criação, prorrogação e consulta de impacto.
- `OPERADOR_PATIO`: ativação e liberação conforme autorização operacional.

### Estados possíveis

`RASCUNHO`, `PROGRAMADA`, `ATIVA`, `PRORROGADA`, `LIBERADA`, `CANCELADA`, `EXPIRADA`.

### Motivos de bloqueio

Geometria inválida, período inconsistente, responsável ausente, conflito com zona ativa, tentativa de dispatch em área bloqueada ou liberação sem confirmação.

### Exemplo

Uma equipe inicia manutenção em três posições de um bloco. A zona é ativada por duas horas, propostas abertas são invalidadas e nenhum novo movimento pode usar origem, destino ou rota dentro da área.

## 5. BUS1880 — Avisos de estivagem

### Finalidade da tela

Manter uma fila acionável de violações físicas do pátio até que a condição deixe de existir.

### Campos

- Unidade e posição.
- Regra violada.
- Chave estável do aviso.
- Severidade.
- Peso, altura, tipo, reefer, perigoso, reserva e capacidade observados.
- Responsável.
- Ação corretiva.
- Resultado da última revalidação.
- Histórico de abertura, atualização, resolução e reabertura.

### Permissões

- `ADMIN_PORTO`: gestão completa.
- `PLANEJADOR`: atribuição e definição da ação corretiva.
- `OPERADOR_PATIO`: execução e evidência da correção.

### Estados possíveis

`ABERTO`, `ATRIBUIDO`, `EM_CORRECAO`, `REVALIDANDO`, `RESOLVIDO`, `REABERTO`.

### Motivos de bloqueio

Condição física ainda presente, movimento corretivo não concluído, regra não revalidada, posição sem leitura atual ou perfil sem permissão.

### Exemplo

Uma unidade excede o limite de peso da pilha. O aviso permanece aberto após a criação da work instruction e só é encerrado quando o movimento termina e a pilha é revalidada sem a violação.

## 6. BUS1890 — Regras e prioridades de appointments

### Finalidade da tela

Configurar conjuntos versionados de regras e grupos de prioridade que expliquem elegibilidade, tolerância, no-show, realocação e override.

### Campos

- Nome, versão e vigência do rule set.
- Tipo de transação.
- Transportadora, carga e critérios de janela.
- Antecedência mínima e máxima.
- Tolerância de check-in antecipado ou tardio.
- Grupo de prioridade.
- Capacidade aplicável.
- Decisão, regra aplicada e motivo.
- Override, justificativa e autorizador.

### Permissões

- `ADMIN_PORTO`: manutenção, ativação e override.
- `PLANEJADOR`: simulação e configuração conforme delegação.
- `OPERADOR_GATE`: consulta da decisão e execução do atendimento permitido.

### Estados possíveis

`RASCUNHO`, `ATIVO`, `INATIVO`, `FUTURO`, `EXPIRADO`, `SUBSTITUIDO`.

### Motivos de bloqueio

Regra sem vigência, conflito entre versões, capacidade esgotada, transação inelegível, check-in fora da tolerância, no-show ou override sem justificativa.

### Exemplo

Uma transportadora prioritária solicita janela para carga perigosa. O simulador mostra a regra aplicada, a capacidade específica e a tolerância; uma rejeição informa exatamente qual condição falhou.

## 7. BUS1900 — Exchange areas do Gate

### Finalidade da tela

Administrar áreas de troca vinculadas a Gates e lanes, com capacidade, serviços aceitos, filas, atraso, indisponibilidade e permanência real.

### Campos

- Código e nome da exchange area.
- Gates e lanes associados.
- Capacidade total, ocupada, reservada e disponível.
- Serviços aceitos.
- Estado operacional.
- Fila e atraso previsto.
- Visita ocupante.
- Entrada, saída e tempo de permanência.
- Motivo de indisponibilidade.

### Permissões

- `ADMIN_PORTO`: configuração e override.
- `PLANEJADOR`: associação, capacidade e indisponibilidade programada.
- `OPERADOR_GATE`: atribuição, transferência e liberação.

### Estados possíveis

`DISPONIVEL`, `PARCIAL`, `LOTADA`, `INDISPONIVEL`, `EM_MANUTENCAO`.

### Motivos de bloqueio

Capacidade esgotada, serviço incompatível, área indisponível, conflito de reserva, visita já ocupando outra área ou transferência não compensada.

### Exemplo

Uma visita que precisa de inspeção é direcionada para uma área com esse serviço. A vaga é reservada sob lock, a entrada registra o início da permanência e a liberação encerra a ocupação sem apagar o histórico.

## 8. BUS1910 — Ciclo de balança

### Finalidade da tela

Capturar, validar, confirmar, rejeitar e repesar leituras de balança antes do avanço da visita.

### Campos

- Truck visit, lane e dispositivo.
- Peso bruto, tara e líquido.
- Unidade de medida.
- Leitura original.
- Operador.
- Ticket.
- Tolerância configurada.
- Divergência calculada.
- Número e histórico de repesagens.
- Estado do dispositivo.
- Override, justificativa e autorizador.

### Permissões

- `ADMIN_PORTO`: configuração, consulta e override.
- `OPERADOR_GATE`: captura, confirmação, rejeição e repesagem.
- `PLANEJADOR`: consulta e análise de divergências.

### Estados possíveis

`AGUARDANDO_LEITURA`, `CAPTURADA`, `VALIDA`, `DIVERGENTE`, `REJEITADA`, `REPESAGEM_SOLICITADA`, `OVERRIDE`, `CONCLUIDA`.

### Motivos de bloqueio

Dispositivo indisponível, leitura ausente, unidade inválida, divergência acima da tolerância, ticket obrigatório ausente, repesagem pendente ou override não autorizado.

### Exemplo

A primeira leitura diverge do peso esperado acima da tolerância. A visita entra em trouble e não avança. Após repesagem válida, a leitura anterior permanece imutável e a nova leitura encerra o bloqueio.

## 9. Atalhos

- `F1`: abrir a ajuda contextual da tela.
- `Shift + ?`: abrir a ajuda contextual.
- `Esc`: fechar ajuda, inspector ou diálogo.
- Atualizar: recarregar estados, filas e conflitos antes de decidir.

## 10. Critérios comuns de auditoria e idempotência

- Todo comando mutável deve receber identificador idempotente.
- Reenvio do mesmo comando não pode duplicar reserva, consumo, ocupação, leitura ou resolução.
- Estado anterior, estado novo, usuário, instante, motivo e snapshot devem ser preservados.
- Casos encerrados não podem ser apagados.
- Overrides exigem permissão explícita e justificativa.
- Revalidação deve comprovar que a condição de bloqueio desapareceu.

## 11. Processo completo e referências

- Requisitos técnicos: `docs/requisitos/requisito-tecnico.md`, seção **2. Gate e pátio**.
- Operação do Gate: `docs/manuais/gate-verificacao-motorista.md` e `docs/manuais/gate-fila-pre-gate.md`.
- Requisitos implementados: `docs/implementados/requisitos-implementados.md`.
