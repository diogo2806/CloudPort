# Operação de navio: prontidão do berço, paralisações e handover

## 1. Finalidade

Este processo impede o início de uma operação de carga sem confirmação física e documental do berço e mantém uma linha do tempo auditável dos eventos reais de cada guindaste.

O checklist de prontidão pertence à escala canônica. Paralisações e handovers pertencem à execução da sequência de guindastes. Os eventos realizados não alteram o plano de estivagem, a sequência planejada, os slots ou as janelas aprovadas.

## 2. Permissões

| Ação | Perfis |
|---|---|
| Consultar escala e prontidão | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_GATE` |
| Confirmar prontidão | `ADMIN_PORTO`, `PLANEJADOR` |
| Iniciar a fase `OPERANDO` | `ADMIN_PORTO`, `PLANEJADOR` |
| Consultar execução e eventos | perfis autorizados pela política de leitura de estiva |
| Registrar ou encerrar paralisação | perfis autorizados pela política de comando de estiva |
| Registrar handover | perfis autorizados pela política de comando de estiva |

O backend valida as permissões. Ocultar ou desabilitar o botão no frontend não substitui autorização do servidor.

## 3. Fluxo operacional

### 3.1 Prontidão do berço

1. A escala deve estar na fase `ATRACADO`.
2. O operador seleciona a escala no Control Room.
3. Informa o berço e o calado operacional.
4. Confirma berço, calado, defensas, amarração, acesso, recursos, avaliação de restrições e liberações.
5. Descreve recursos, restrições, liberações e observações relevantes.
6. Confirma o checklist.
7. O backend cria uma nova versão imutável, preservando todas as anteriores.
8. A escala somente pode avançar para `OPERANDO` quando a versão mais recente estiver `PRONTO`.

### 3.2 Paralisação planejada

1. Carregue a execução pelo identificador do plano de estiva.
2. Selecione o guindaste.
3. Escolha a natureza `PLANEJADA`.
4. Informe início e fim, motivo, impacto, turno e pendências.
5. Confirme o registro.
6. O intervalo não pode se sobrepor a outra paralisação do mesmo guindaste.
7. Movimentos cujo instante de início esteja dentro da janela ficam bloqueados.

### 3.3 Paralisação operacional

1. Selecione a natureza `OPERACIONAL`.
2. Informe o instante da ocorrência, motivo, impacto e turno.
3. O fim pode permanecer vazio enquanto a falha estiver ativa.
4. O banco permite apenas uma paralisação aberta por execução e guindaste.
5. Após a liberação física, use **Encerrar** na linha do tempo.
6. O fim deve ser posterior ao início e não pode gerar sobreposição.

### 3.4 Handover de turno

1. Informe guindaste e instante da passagem.
2. Registre turno de origem e turno de destino.
3. Informe o responsável que recebe a operação.
4. Descreva todas as pendências, inclusive bays, tampas, equipamentos, avarias, cargas especiais e restrições.
5. Confirme o handover.
6. O evento fica registrado como `REGISTRADO` e não modifica o plano.

## 4. Campos

### 4.1 Checklist do berço

| Campo | Uso |
|---|---|
| Berço | posição física efetivamente utilizada pela escala |
| Calado operacional | calado confirmado para a condição de atendimento |
| Berço confirmado | disponibilidade e identificação do berço |
| Calado confirmado | compatibilidade do calado com acesso e permanência |
| Defensas confirmadas | condição das defensas e proteção do casco |
| Amarração confirmada | cabos, pontos e condição de amarração |
| Acesso confirmado | escada, passarela, isolamento e circulação segura |
| Recursos confirmados | pessoal, equipamentos e apoio disponíveis |
| Restrições avaliadas | limitações conhecidas analisadas e comunicadas |
| Liberações confirmadas | autorizações operacionais e documentais obtidas |
| Observações | informação complementar que não substitui uma confirmação crítica |

### 4.2 Paralisação

| Campo | Uso |
|---|---|
| Guindaste | identificador do equipamento dentro da execução |
| Natureza | `PLANEJADA` ou `OPERACIONAL` |
| Início e fim | intervalo real ou programado da indisponibilidade |
| Motivo | causa objetiva do evento |
| Impacto | efeito esperado ou realizado na operação |
| Turno | equipe responsável no momento da ocorrência |
| Pendências | tarefas e verificações ainda abertas |

### 4.3 Handover

| Campo | Uso |
|---|---|
| Turno de origem | equipe que entrega a operação |
| Turno de destino | equipe que recebe a operação |
| Responsável de destino | operador responsável pelo aceite |
| Pendências | memória operacional obrigatória da passagem |
| Observações | contexto complementar |

## 5. Estados possíveis

### Prontidão

- `PENDENTE`: uma ou mais confirmações críticas estão ausentes.
- `PRONTO`: todas as confirmações críticas da versão mais recente estão positivas.

### Escala

- `ATRACADO`: permite registrar prontidão.
- `OPERANDO`: somente alcançado após validação bloqueante do checklist.

### Eventos de guindaste

- Paralisação `ABERTA`: sem instante final e bloqueante para novos movimentos.
- Paralisação `ENCERRADA`: possui início e fim.
- Handover `REGISTRADO`: passagem de turno auditada.

### Execução

- `PLANEJADA`
- `EM_EXECUCAO`
- `AGUARDANDO_RECONCILIACAO`
- `RECONCILIADA`

## 6. Motivos de bloqueio

O início da escala é bloqueado quando:

- a escala não está `ATRACADO`;
- não existe checklist de prontidão;
- berço ou calado não estão confirmados;
- defensas, amarração ou acesso não estão confirmados;
- recursos não estão confirmados;
- restrições não foram avaliadas;
- liberações não foram confirmadas.

O registro ou uso do guindaste é bloqueado quando:

- a execução ou o guindaste não existe;
- uma paralisação planejada não informa fim;
- o fim é igual ou anterior ao início;
- o intervalo sobrepõe outra paralisação do mesmo guindaste;
- já existe paralisação aberta para o equipamento;
- o movimento é iniciado durante paralisação ativa;
- o evento informado não pertence à execução.

## 7. Exemplos

### Checklist pronto

- Berço: `B03`
- Calado: `12,350 m`
- Recursos: `QC-01, QC-02, equipe de amarração e apoio de segurança disponíveis`
- Restrições: `rajadas monitoradas; limite comunicado ao supervisor`
- Liberações: `autoridade portuária, terminal e segurança operacional`

### Paralisação operacional

- Guindaste: `1`
- Natureza: `OPERACIONAL`
- Motivo: `Falha no spreader`
- Impacto: `Interrupção do bay 14 e transferência temporária de prioridade para o QC-02`
- Pendência: `Teste funcional e liberação da manutenção`

### Handover

- Origem: `TURNO_A`
- Destino: `TURNO_B`
- Responsável de destino: `operador-02`
- Pendências: `Concluir bay 14, conferir tampa do porão e validar dois contêineres reefer`

## 8. Atalhos

- `Alt + E`: posiciona na seleção da escala.
- `Alt + P`: posiciona no identificador do plano de estiva.
- **Atualizar prontidão**: recarrega checklist e histórico da escala.
- **Atualizar execução**: recarrega execução e linha do tempo do plano informado.
- **Encerrar**: encerra a paralisação aberta selecionada no instante atual.

## 9. API

### Prontidão

- `GET /escalas/{id}/prontidao-berco`
- `GET /escalas/{id}/prontidao-berco/historico`
- `POST /escalas/{id}/prontidao-berco`
- `PATCH /escalas/{id}/fase` com `OPERANDO`

### Eventos de guindaste

- `GET /api/vessel-planner/planos/{id}/execucao-guindastes`
- `GET /api/vessel-planner/execucoes-guindastes/{execucaoId}/eventos-operacionais`
- `POST /api/vessel-planner/execucoes-guindastes/{execucaoId}/paralisacoes`
- `POST /api/vessel-planner/execucoes-guindastes/{execucaoId}/paralisacoes/{eventoId}/encerrar`
- `POST /api/vessel-planner/execucoes-guindastes/{execucaoId}/handovers`

## 10. Persistência e auditoria

- Cada confirmação de prontidão recebe uma versão sequencial por escala.
- Versões anteriores não são sobrescritas.
- Responsável e instante de confirmação são persistidos.
- Eventos de guindaste possuem versão otimista, responsável, instantes e dados operacionais.
- O encerramento registra responsável e observação próprios.
- A execução mantém o plano aprovado separado dos eventos realizados.
- A reconciliação posterior continua utilizando os movimentos reais sem perda do histórico de paralisação e handover.
