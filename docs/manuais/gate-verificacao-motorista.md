# Manual operacional — verificação do motorista no Gate

## Finalidade da tela

A tela **Gate > Operação completa** valida a identidade operacional do motorista antes de permitir o avanço da truck visit ou a autorização de entrada. A conferência fica vinculada ao motorista, à transportadora e à visita ou ao agendamento em processamento.

## Fluxo operacional

1. Selecione a truck visit na grade.
2. Confira motorista, transportadora, placa e estágio atual.
3. Na seção **Verificação operacional**, escolha `DOCUMENTO`, `PIN` ou `CREDENCIAL`.
4. Informe o valor apresentado pelo motorista e selecione **Verificar motorista**.
5. Confirme que o estado mudou para `VERIFICADA`.
6. Conclua as business tasks obrigatórias do estágio.
7. Selecione **Concluir estágio e avançar**.
8. Quando a conferência normal não puder ser concluída, um administrador pode registrar um override com justificativa obrigatória.

A mesma verificação é exigida pelo backend no processamento da entrada. A desativação ou manipulação do botão no navegador não ignora o bloqueio operacional.

## Explicação dos campos

| Campo | Explicação |
|---|---|
| Método | Forma de conferência: documento cadastrado, PIN operacional ou credencial do motorista. |
| Valor | Informação apresentada pelo motorista. PIN e credencial são mascarados na tela. |
| Estado | Situação atual da conferência vinculada à operação. |
| Método aprovado | Método usado na última aprovação ou no override. |
| Tentativas restantes | Quantidade disponível antes do bloqueio temporário. |
| Verificado em | Instante em que a conferência foi aprovada. |
| Expira em | Limite de validade da autorização operacional. |
| Bloqueado até | Instante a partir do qual uma nova tentativa poderá ser realizada. |
| Motivo do override | Justificativa auditável informada pelo administrador. |

## Permissões necessárias

- `OPERADOR_GATE`: consulta e executa verificação por documento, PIN ou credencial.
- `ADMIN_PORTO`: possui as mesmas ações e pode cadastrar credenciais e autorizar override motivado.
- `PLANEJADOR`: consulta o estado, mas não valida nem autoriza override.

## Estados possíveis

- `PENDENTE`: nenhuma conferência válida está disponível.
- `VERIFICADA`: identidade aprovada e dentro do período de validade.
- `BLOQUEADA`: limite de tentativas inválidas atingido; novas tentativas permanecem impedidas temporariamente.
- `EXPIRADA`: a conferência foi aprovada anteriormente, mas perdeu a validade.
- `OVERRIDE`: administrador autorizou o avanço mediante justificativa registrada.

## Motivos de bloqueio

- Motorista não corresponde ao documento, PIN ou credencial informada.
- Transportadora da credencial diverge da transportadora da visita.
- Credencial inexistente, revogada, bloqueada, ainda não vigente ou expirada.
- Três tentativas inválidas na mesma operação.
- Verificação aprovada há mais de 30 minutos.
- Truck visit ou agendamento não encontrado.
- Perfil autenticado sem a permissão necessária.
- Override sem justificativa suficiente.
- Business task obrigatória do estágio ainda não concluída.
- Trouble transaction aberta para a visita.

## Exemplos

### Documento aprovado

O operador seleciona `DOCUMENTO`, informa o documento apresentado e recebe o estado `VERIFICADA`. O botão de avanço fica disponível quando as tarefas obrigatórias também estiverem concluídas.

### PIN inválido

O operador informa um PIN incorreto. A tentativa é persistida e a tela mostra a quantidade restante. Ao atingir três falhas, o estado muda para `BLOQUEADA` por 15 minutos.

### Override autorizado

Uma indisponibilidade comprovada impede a conferência normal. Um usuário `ADMIN_PORTO` registra uma justificativa detalhada. O estado muda para `OVERRIDE`, com usuário, motivo e instante preservados para auditoria.

## Atalhos

- `F1`: abre a ajuda contextual da tela.
- `Shift + ?`: abre a ajuda contextual da tela.
- `Esc`: fecha o painel de ajuda.
- **Inspecionar**: seleciona a truck visit e abre os dados operacionais.
- **Atualizar**: recarrega painel, visitas e situação operacional.

## Processo completo

1. Cadastro do motorista e vínculo com a transportadora.
2. Cadastro administrativo de PIN ou credencial, quando aplicável.
3. Criação do agendamento e da truck visit.
4. Conferência operacional da identidade.
5. Execução das tarefas do estágio.
6. Avanço da visita ou processamento da entrada.
7. Persistência das tentativas, bloqueios, aprovações e overrides.
8. Consulta posterior para auditoria e investigação de divergências.

Referências técnicas:

- `backend/servico-gate/src/main/java/br/com/cloudport/servicogate/app/verificacao/`
- `backend/servico-gate/src/main/resources/db/migration/V306__verificacao_operacional_motorista.sql`
- `frontend/cloudport/src/pages/GateOperationsPage.jsx`
- `docs/requisitos/requisito-tecnico.md#bus1290--arquivos-e-métodos`
