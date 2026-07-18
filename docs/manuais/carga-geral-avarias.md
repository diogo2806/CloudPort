# Manual operacional — avarias de carga geral

## Finalidade da tela

O inspector de avarias da tela **Carga geral e break-bulk** controla o ciclo completo de danos identificados em um cargo lot. Ele registra a parcela afetada, segrega essa parcela do saldo disponível, mantém evidências e responsáveis e conduz o caso até o reparo ou a baixa definitiva.

O saldo total do lote não é substituído pelo registro da avaria. A quantidade, o volume e o peso afetados permanecem identificados como saldo segregado e deixam de estar disponíveis para entrega, carga, transferência ou reserva operacional enquanto o caso estiver ativo.

## Fluxo operacional

1. Na grade **Inventário de cargo lots**, localize o lote e selecione **Operar**.
2. Confira no inspector os indicadores de saldo total, segregado e disponível.
3. Informe código, descrição, quantidade, volume, peso e responsável pela avaria.
4. Selecione **Abrir avaria**. O sistema cria um comando idempotente e bloqueia somente a parcela afetada.
5. Selecione a avaria na grade e adicione fotos, vídeos, laudos ou referências documentais.
6. Inicie a inspeção com a ação `INSPECIONAR`.
7. Após a avaliação, escolha um dos caminhos:
   - `REPARAR`, seguido de `CONCLUIR_REPARO`, para reintegrar o saldo segregado ao saldo disponível;
   - `BAIXAR`, para retirar definitivamente a parcela afetada do estoque lógico.
8. Confira o novo saldo disponível e o histórico da avaria.

## Explicação dos campos

### Abertura da avaria

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Código da avaria | Sim | Classificação operacional do dano, como `EMBALAGEM_RASGADA`, `UMIDADE` ou `QUEBRA`. |
| Descrição | Sim | Relato objetivo do dano, da forma como foi identificado e da área afetada. |
| Quantidade afetada | Sim | Parcela do saldo do lote que será segregada. Deve ser maior que zero. |
| Volume afetado m³ | Sim | Volume correspondente à parcela afetada. Aceita zero quando o controle não se aplica. |
| Peso afetado kg | Sim | Peso correspondente à parcela afetada. Aceita zero quando o controle não se aplica. |
| Responsável | Sim | Usuário ou equipe responsável pela abertura e acompanhamento inicial. |

### Evidências

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Tipo de evidência | Sim | `FOTO`, `VIDEO`, `LAUDO`, `DOCUMENTO` ou `OUTRO`. |
| URI da evidência | Sim | Endereço seguro ou referência documental que permita localizar a evidência. A tela registra a referência, não transfere o arquivo. |
| Checksum | Não | Hash utilizado para comprovar integridade do arquivo referenciado. |
| Responsável | Sim | Pessoa que registrou ou conferiu a evidência. |

### Transição do caso

| Campo | Obrigatório | Explicação |
|---|---:|---|
| Próxima ação | Sim | Ação permitida para o estado atual da avaria. |
| Usuário | Sim | Responsável pela decisão operacional. |
| Observação | Não | Laudo resumido, motivo da decisão ou instrução de reparo e baixa. |

## Permissões necessárias

Os endpoints do ciclo de avarias são acessíveis aos perfis:

- `ADMIN_PORTO`;
- `PLANEJADOR`;
- `OPERADOR_GATE`.

A autorização continua sujeita às regras de autenticação e ao escopo configurado no ambiente. Usuários sem um dos perfis recebem bloqueio de acesso antes da execução da operação.

## Estados possíveis

| Estado | Significado | Próximas ações |
|---|---|---|
| `ABERTA` | Estado de compatibilidade para registros anteriores. | Inspecionar ou baixar. |
| `BLOQUEADA` | Caso criado e saldo afetado segregado. É o estado inicial atual. | Inspecionar ou baixar. |
| `EM_INSPECAO` | Dano em avaliação técnica ou operacional. | Iniciar reparo ou baixar. |
| `EM_REPARO` | Parcela afetada em tratamento. | Concluir reparo ou baixar. |
| `REPARADA` | Reparo concluído e saldo reintegrado à disponibilidade. | Estado final. |
| `BAIXADA` | Parcela removida definitivamente do estoque. | Estado final. |

## Motivos de bloqueio

A operação é rejeitada quando:

- nenhum cargo lot foi selecionado;
- a quantidade, o volume ou o peso afetado excede o saldo disponível e não bloqueado;
- a quantidade afetada é zero ou negativa;
- um campo obrigatório não foi informado;
- o mesmo comando de abertura é reenviado com dados incompatíveis;
- a transição não corresponde ao estado atual, como concluir um reparo antes de iniciá-lo;
- uma evidência é adicionada depois que a avaria foi reparada ou baixada;
- a baixa ou liberação excede o saldo que permanece bloqueado;
- o usuário não possui uma das permissões exigidas;
- o cargo lot ou a avaria não existe.

## Exemplos

### Embalagens rasgadas com reparo

Um lote possui saldo de 100 unidades. Dez embalagens são encontradas rasgadas.

1. Abra a avaria com quantidade afetada `10`.
2. O inspector mostrará saldo total `100`, segregado `10` e disponível `90`.
3. Adicione fotografias e o laudo de inspeção.
4. Execute `INSPECIONAR`, `REPARAR` e `CONCLUIR_REPARO`.
5. O saldo segregado volta a zero e o disponível retorna para `100`.

### Perda por umidade com baixa

Um lote possui 5.000 kg, dos quais 300 kg foram inutilizados.

1. Abra a avaria com peso afetado `300` e a quantidade operacional correspondente.
2. Adicione o laudo e a referência do registro fotográfico.
3. Execute `INSPECIONAR` e depois `BAIXAR`.
4. O sistema remove a parcela bloqueada do saldo total e registra uma movimentação de ajuste vinculada à avaria.

## Atalhos

- **Operar**: seleciona o cargo lot diretamente na grade de inventário.
- **Inspecionar**: seleciona uma avaria para exibir evidências e ações permitidas.
- **Atualizar**: recarrega dashboard, lotes e saldos consolidados.
- **Exportar**: as grades de lotes e avarias podem ser exportadas pelos controles da tabela.
- **Manual**: abre este documento em uma nova aba.

## Processo completo e contratos

- Processo técnico completo: [BUS1070 — ciclo completo de avaria da carga](../requisitos/requisito-tecnico.md#bus1070--arquivos-e-métodos).
- Abertura e listagem: `POST/GET /api/carga-geral/operacoes-intermodais/avarias`.
- Evidências: `POST /api/carga-geral/operacoes-intermodais/avarias/{id}/evidencias`.
- Inspeção, reparo e baixa: `POST /api/carga-geral/operacoes-intermodais/avarias/{id}/transicoes`.

O endpoint simplificado `POST /api/carga-geral/lotes/{id}/avarias` foi desativado porque não representa quantidade afetada nem segregação de saldo.
