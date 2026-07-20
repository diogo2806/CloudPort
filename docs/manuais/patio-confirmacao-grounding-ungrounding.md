# Confirmação física de grounding e ungrounding no pátio

## Finalidade

Garantir que uma work instruction somente seja concluída depois da comprovação física da retirada ou da colocação da unidade. A confirmação reúne leitura da unidade, CHE, origem, destino, posição, sequência, operador, instante e chave idempotente.

## Fluxo operacional

1. A work instruction é vinculada a uma work queue com CHE real e recursos operacionais completos.
2. O dispatch coloca a instrução em execução.
3. O VMT registra aceite e início em ordem cronológica.
4. O operador seleciona a instrução em **Pátio > Lista de trabalho**.
5. O operador abre **Confirmar transferência física**.
6. A unidade e o CHE são lidos novamente.
7. Origem, destino, posição e sequência são conferidos.
8. O backend bloqueia a instrução para escrita, valida a job list e verifica o inventário.
9. No grounding, a unidade é registrada na posição de destino.
10. No ungrounding, a unidade é retirada da posição atual.
11. Inventário, evento VMT, histórico e conclusão da instrução são persistidos na mesma transação.

## Campos

| Campo | Explicação |
|---|---|
| Ação física | `GROUNDING` coloca a unidade em uma posição; `UNGROUNDING` retira a unidade da posição atual. |
| Unidade lida | Código físico conferido no momento da execução. Deve ser igual ao código da work instruction. |
| ID do CHE | Identificador interno do equipamento associado à work queue. |
| Identificador do CHE | Código operacional visível do equipamento, como `RTG-01`. |
| Origem | Área operacional de onde a unidade está sendo retirada ou recebida. |
| Destino | Área prevista pela work instruction. |
| Linha, coluna e camada de origem | Posição atual do inventário, obrigatória no ungrounding e validada quando a unidade já está inventariada. |
| Linha, coluna e camada de destino | Posição definida pela work instruction, obrigatória no grounding. |
| Sequência operacional | Ordem oficial da instrução dentro da job list. |
| Evento | Chave única usada para impedir processamento repetido. |
| Timestamp | Instante do evento, que precisa ser posterior ao último evento VMT da instrução. |

## Permissões necessárias

- `ADMIN_PORTO`: consulta e confirmação.
- `PLANEJADOR`: consulta e confirmação.
- `OPERADOR_PATIO`: consulta e confirmação.
- `INTEGRACAO_VMT`: envio pela integração VMT.

A autorização é validada novamente pelo backend.

## Estados possíveis

### Work instruction

- `PENDENTE`
- `EM_EXECUCAO`
- `SUSPENSA`
- `BLOQUEADA`
- `CONCLUIDA`
- `CANCELADA`

### Confirmação VMT

- `PENDENTE`
- `ACEITA`
- `EM_EXECUCAO`
- `FALHA`
- `CONCLUIDA`

A transferência física só pode ser confirmada quando a instrução e o ciclo VMT estão em execução.

## Motivos de bloqueio

- Evento repetido.
- Timestamp igual ou anterior ao último evento.
- Estado VMT fora de sequência.
- Work instruction não despachada.
- Unidade lida diferente da unidade da instrução.
- Work queue ausente.
- CHE não associado, inexistente, divergente ou indisponível.
- Ação física incompatível com o movimento.
- Sequência informada diferente da sequência da instrução.
- Instrução anterior da job list ainda aberta.
- Origem diferente da posição atual do inventário.
- Destino diferente da posição prevista.
- Unidade ausente do inventário em uma tentativa de ungrounding.

Nenhuma dessas falhas pode alterar parcialmente o inventário.

## Exemplos

### Grounding

A unidade `CONT-001` está sendo recebida da exchange area. A work instruction determina o bloco `A`, linha `10`, coluna `3`, camada `T2`, sequência `4`, usando o CHE `RTG-01`. Após a conferência, a unidade é colocada nessa posição, o evento é persistido e a instrução é concluída.

### Ungrounding

A unidade `CONT-002` está armazenada na linha `8`, coluna `1`, camada `T1`. A leitura confirma a unidade, a posição atual e o CHE associado. O sistema remove o vínculo da posição, atualiza o destino operacional e conclui a instrução.

### Tentativa repetida

O mesmo `eventId` é reenviado após uma perda de conexão. O backend rejeita o evento duplicado antes de bloquear ou alterar a unidade.

## Atalhos

- `F1` ou `Shift + ?`: abrir a ajuda contextual quando o atalho global estiver ativo.
- `Esc`: cancelar o formulário sem enviar a confirmação.
- **Atualizar**: recarregar work queues, work instructions e estado persistido.

## Processo completo

O processo relacionado começa na criação da work instruction, passa pela associação de recursos e dispatch, segue pelo ciclo VMT e termina na confirmação física e reconciliação do inventário. Consulte também a seção **Yard, inventário e dispatch** em `docs/implementados/requisitos-implementados.md`.
