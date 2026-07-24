# Avisos de estivagem do pátio

## Objetivo

O ciclo de avisos de estivagem registra e acompanha violações físicas do inventário do pátio sem apagar o histórico. Cada caso possui uma chave estável formada por unidade, posição e regra. A mesma ocorrência não gera duplicidade; quando volta após uma resolução, o caso original é reaberto e o contador de ocorrências é incrementado.

## Regras detectadas

A varredura avalia os dados persistidos da unidade, da posição e da pilha:

- peso acima do limite estrutural da posição;
- camada acima da altura máxima configurada;
- tipo de carga incompatível;
- reefer fora de posição compatível;
- carga perigosa sem o isolamento operacional mínimo;
- capacidade da pilha excedida;
- ocupação incompatível com reserva ativa;
- ausência de apoio físico imediatamente abaixo;
- posição bloqueada, interditada ou fora da área permitida;
- unidade mais pesada acima de unidade mais leve.

## Estados

1. `ABERTO`: violação detectada pela primeira vez.
2. `ATRIBUIDO`: responsável e prazo registrados.
3. `EM_CORRECAO`: ação corretiva iniciada e evidenciada.
4. `AGUARDANDO_REVALIDACAO`: correção concluída, mas a condição física ainda precisa ser conferida.
5. `RESOLVIDO`: a revalidação confirmou que a violação deixou de existir.
6. `REABERTO`: a mesma violação permaneceu ou voltou a ocorrer.

## Fluxo operacional

1. Abra o mapa do pátio e selecione **Avisos de estivagem**.
2. Execute **Varrer inventário** para sincronizar a fotografia física com os casos persistidos.
3. Priorize a fila por severidade, responsável e prazo.
4. Atribua responsável e prazo ao caso.
5. Registre a ação corretiva e a evidência.
6. Envie o caso para revalidação.
7. Execute a revalidação da condição física.
8. Consulte o histórico para conferir ator, transição, detalhes, evidência, resultado e data.

A conclusão de uma instrução de trabalho não altera o aviso apenas por mudança de status. Depois da movimentação física, origem, destino, unidade e vizinhança são novamente avaliados. O caso só é resolvido quando a regra deixa de ser violada.

## Bloqueios críticos

Avisos com severidade `CRITICA` bloqueiam novos planejamentos de movimentação e o dispatch da ordem incompatível. O bloqueio permanece enquanto o caso estiver em qualquer estado ativo:

- `ABERTO`;
- `ATRIBUIDO`;
- `EM_CORRECAO`;
- `AGUARDANDO_REVALIDACAO`;
- `REABERTO`.

Para liberar a operação, corrija a condição e execute a revalidação. Alterar apenas o responsável, o prazo ou o estado da instrução não remove o bloqueio.

## Badges e priorização

O resumo apresenta contagens ativas por:

- bloco;
- pilha;
- posição;
- unidade.

O mapa também apresenta a quantidade total e o número de casos críticos. A fila permite filtrar casos atrasados, com prazo nas próximas 24 horas ou sem prazo definido.

## API

Base: `/api/yard/stowage-warnings`

- `GET /`: lista a fila.
- `GET /summary`: retorna badges e métricas.
- `GET /{id}/history`: retorna o histórico do caso.
- `POST /scan`: varre o inventário.
- `POST /unit/{codigoUnidade}/revalidate`: reavalia uma unidade.
- `POST /{id}/assign`: atribui responsável e prazo.
- `POST /{id}/start-correction`: inicia a correção.
- `POST /{id}/submit-revalidation`: envia para revalidação.
- `POST /{id}/revalidate`: revalida a condição física.

## Auditoria

Cada evento mantém:

- estado anterior e novo;
- tipo de evento;
- ator;
- detalhes da transição;
- evidência;
- resultado;
- data e hora.

O histórico não é substituído durante atualizações, resoluções ou reaberturas.
