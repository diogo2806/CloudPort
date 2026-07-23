# Zonas temporárias de segurança do pátio

## Finalidade

A zona temporária representa uma área operacional indisponível durante trabalho humano, manutenção, inspeção ou outra condição de risco. Enquanto ativa, impede o dispatch para posições afetadas e reavalia instruções já abertas.

## Fluxo operacional

1. Cadastre a zona em `POST /yard/zonas-seguranca`, informando chave de idempotência, nome, posições, vigência, responsável, equipe e motivo.
2. Ative em `POST /yard/zonas-seguranca/{id}/ativar`.
3. A ativação localiza instruções pendentes, suspensas ou em execução cujo destino pertença à zona, registra conflitos e bloqueia as que ainda não iniciaram.
4. O banco impede qualquer transição posterior para `EM_EXECUCAO` quando o destino estiver em zona ativa.
5. Prorrogue em `POST /yard/zonas-seguranca/{id}/prorrogar`.
6. Libere em `POST /yard/zonas-seguranca/{id}/liberar`. O histórico, o responsável e o período efetivo permanecem gravados.

## Estados

- `RASCUNHO`: cadastrada, ainda sem bloqueio operacional.
- `ATIVA`: vigência aplicada ao planejamento e dispatch.
- `EXPIRADA`: fim da vigência alcançado.
- `LIBERADA`: encerrada manualmente com motivo e operador.

## Idempotência e concorrência

A chave de idempotência é única. Uma criação repetida devolve o cadastro existente. Ativar ou liberar novamente uma zona no mesmo estado não duplica eventos nem conflitos. A versão do agregado cresce em cada mudança operacional e a tabela de eventos possui unicidade por zona e versão.

## Auditoria

Cada criação, ativação, prorrogação e liberação registra versão, operador, correlação, data e payload. Os conflitos com work instructions permanecem associados à zona e são apenas marcados como resolvidos na liberação.

## Motivos de bloqueio

O dispatch retorna conflito quando a posição de destino pertence a uma zona ativa e vigente. A validação de domínio também aceita origem e pontos de rota para integração pelos planejadores e roteadores.

## Permissões

As rotas exigem autenticação pela configuração geral do módulo. A matriz granular deve conceder cadastro e ativação somente a perfis operacionais autorizados, mantendo consulta disponível ao Control Room.