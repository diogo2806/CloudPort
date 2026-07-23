# Modo operador — celular, tablet industrial e PDA

## Finalidade

O modo operador oferece uma interface de campo simplificada para executar tarefas curtas de Gate, Pátio, Ferrovia e Inventário. Ele mantém uma tarefa por vez, reduz navegação, prioriza leitura física e deixa conectividade e sincronização sempre visíveis.

O modo compacto não substitui as telas completas. Ações que exigem formulários extensos, evidências, justificativas adicionais ou regras não representadas no canal de campo são encaminhadas ao módulo operacional correspondente.

## Como abrir

Em qualquer tela que utiliza o cabeçalho padrão do portal, selecione **Operador**. O workspace ocupa toda a área disponível e pode ser fechado por **Voltar ao modo completo**, pelo botão `×` ou pela tecla `Esc`.

## Fluxo operacional

1. Confira se o dispositivo está online ou offline.
2. Verifique a última sincronização, as operações pendentes e eventuais conflitos.
3. Selecione a tarefa atual ou outra tarefa da fila autorizada.
4. Leia o objeto físico com scanner, câmera ou entrada manual.
5. Corrija a leitura quando o sistema informar o motivo e a orientação.
6. Revise ação, origem, destino, equipamento e referência.
7. Confirme uma vez.
8. Quando offline, acompanhe o comando na fila local.
9. Após a reconexão, selecione **Sincronizar fila**.
10. Em falha ou conflito, repita, descarte com motivo ou abra o modo completo para conferir o estado real.

## Tarefas e priorização

As tarefas são construídas somente com fontes autorizadas para a sessão:

| Fonte | Papéis | Dados usados |
| --- | --- | --- |
| Gate | `ADMIN_PORTO`, `OPERADOR_GATE`, `PLANEJADOR` | central do Gate |
| Pátio | `ADMIN_PORTO`, `OPERADOR_PATIO`, `PLANEJADOR` | ordens do pátio |
| Ferrovia | `ADMIN_PORTO`, `OPERADOR_PATIO`, `PLANEJADOR` | visitas e ordens ferroviárias |
| Inventário | `ADMIN_PORTO`, `OPERADOR_PATIO`, `PLANEJADOR`, `OPERADOR_GATE` | equipamentos e unidades do inventário |

A ordem considera primeiro bloqueios, falhas, divergências e atrasos; depois tarefas em execução ou processamento; por fim itens pendentes. Dentro da mesma prioridade, o prazo mais próximo aparece antes.

## Campos

- **Online/Offline:** estado informado pelo navegador.
- **Última sincronização:** horário da última carga de tarefas.
- **Operações pendentes:** comandos ainda não concluídos ou descartados.
- **Tarefa atual:** ação em foco.
- **Próxima tarefa:** próximo item priorizado.
- **Estado:** situação retornada pela fonte operacional.
- **Referência:** contêiner, placa, tarefa ou outro identificador.
- **Origem e destino:** locais físicos associados à execução.
- **Equipamento:** CHE, vagão, pista ou recurso relacionado.
- **Prazo:** horário operacional disponível na fonte.
- **Leitura:** valor digitado ou recebido por scanner/câmera.
- **Objeto lido:** tipo e valor validados.
- **Fila de sincronização:** comandos locais, tentativas, erros e conflitos.

## Scanner físico

Coletores configurados como teclado enviam os caracteres ao campo **Scanner físico ou entrada manual**. Posicione o foco no campo, faça a leitura e envie `Enter`.

A validação é a mesma usada pela câmera e pela digitação manual. O canal de entrada não altera a regra.

## Câmera

Quando o navegador oferece `BarcodeDetector` e permite `getUserMedia`, o botão **Usar câmera** abre a câmera traseira. O stream pertence ao componente de câmera e é encerrado automaticamente após uma leitura, fechamento, troca de tarefa ou saída do modo operador.

Se a câmera não estiver disponível, scanner físico e entrada manual continuam ativos.

## Formatos reconhecidos

### Contêiner

Formato ISO 6346: quatro letras, seis dígitos de série e um dígito verificador. O dígito é recalculado antes da confirmação.

Exemplo válido: `MSCU6639870`.

### Placa

São aceitos os formatos brasileiros:

- antigo: `ABC1234`;
- Mercosul: `ABC1D23`.

### Posição

Aceita identificadores com separadores operacionais, como `A-01-02`, `BLOCO/01` ou `PILHA.03`.

### Tarefa

Aceita prefixos como `TASK`, `TAREFA`, `WI` e `ORDEM` seguidos de identificador.

### QR estruturado

Pode ser JSON, URL com parâmetros ou texto prefixado. Exemplos:

```json
{"type":"CONTAINER","value":"MSCU6639870"}
```

```text
CONTAINER:MSCU6639870
PLACA:ABC1D23
POSICAO:A-01-02
```

## Validações e mensagens corretivas

- contêiner com dígito inválido: informa a regra ISO 6346;
- placa fora dos padrões aceitos: pede sete caracteres;
- posição incompleta: solicita bloco e posição completa;
- formato desconhecido: lista os tipos aceitos;
- tipo diferente do esperado: informa o tipo esperado e o lido;
- referência diferente da tarefa: mostra ambos os valores e bloqueia a confirmação.

## Confirmação crítica

Antes da ação, o resumo exibe:

- ação;
- objeto lido;
- origem;
- destino;
- equipamento.

O botão de confirmação permanece desabilitado até existir leitura válida. Em tarefas de Gate ou Inventário que exigem mais contexto, o workspace abre a tela completa em vez de criar uma alteração incompleta.

## Idempotência no cliente

Cada comando recebe uma chave determinística formada por fonte, tarefa, visita, ação e leitura. Um segundo toque ou nova tentativa enquanto o mesmo comando está pendente não cria outra entrada na fila.

A idempotência do cliente reduz duplicidade acidental, mas não substitui a validação do backend. As APIs continuam responsáveis por autorização, estado atual e consistência.

## Operação offline

Quando offline:

- a interface não considera a operação concluída;
- comandos compatíveis são salvos como **Aguardando conexão**;
- nenhuma liberação dependente de validação online é aplicada localmente;
- após a reconexão, o usuário revisa e sincroniza a fila;
- o backend revalida estado e autorização no envio.

A fila utiliza armazenamento local separado por usuário. Limpeza do navegador pode remover comandos ainda não enviados; por isso o indicador de pendências deve ser conferido antes de encerrar o turno ou limpar dados do dispositivo.

## Estados da fila

- `PENDENTE`: pronto para tentativa online;
- `AGUARDANDO_RECONEXAO`: criado sem conexão;
- `ENVIANDO`: chamada em andamento;
- `CONCLUIDA`: backend confirmou a operação; removido da fila ativa;
- `FALHA`: erro transitório ou de integração;
- `CONFLITO`: estado, versão ou duplicidade precisa de conferência;
- `DESCARTADA`: removido localmente com motivo.

## Falhas e conflitos

Uma falha pode ser repetida. Um conflito deve ser conferido no modo completo antes de repetir ou descartar. O descarte exige motivo e não altera o estado físico nem o registro do backend.

## Impedimentos

**Informar impedimento** prepara o contexto da tarefa e abre caminho para o registro auditável no modo completo. O modo compacto não inventa um endpoint genérico nem considera uma anotação local como evidência persistida.

## Permissões

O frontend carrega apenas fontes permitidas ao papel. O backend repete a autorização em cada comando. Favoritos, fila local, leitura de QR ou acesso ao workspace não concedem novas permissões.

## Estados da tela

- carregando tarefas;
- pronta com tarefa atual;
- sem tarefa disponível;
- fonte parcial indisponível;
- todas as fontes indisponíveis;
- leitura válida;
- leitura inválida;
- câmera ativa;
- online;
- offline;
- sincronizando;
- conflito pendente.

## Motivos de bloqueio

- perfil sem acesso à fonte;
- formato de leitura inválido;
- dígito ISO 6346 inválido;
- objeto lido diferente da tarefa;
- leitura obrigatória ausente;
- estado incompatível no backend;
- registro alterado por outro usuário;
- conexão necessária para validação;
- comando idêntico já pendente;
- tarefa que exige modo completo;
- câmera não suportada ou sem permissão;
- armazenamento local indisponível.

## Acessibilidade e ergonomia

- alvos principais possuem pelo menos 48 px;
- o layout funciona a partir de 320 px sem rolagem horizontal da página;
- há modo de alto contraste;
- estados são escritos em texto, não apenas em cor;
- feedback sonoro pode ser desligado;
- foco possui contorno visível;
- o workspace é um diálogo modal nomeado;
- retrato e paisagem são suportados;
- movimento reduzido desativa a animação do carregamento.

## Atalhos

- `Enter`: valida o campo de leitura;
- `Esc`: fecha câmera, manual ou workspace;
- `Tab` e `Shift + Tab`: percorrem controles;
- scanner físico: envia caracteres ao campo e normalmente finaliza com `Enter`.

## Exemplo completo

Uma ordem de pátio pede a movimentação de `MSCU6639870` da posição `A-01-02` para `B-04-01` com o CHE `RTG-02`. O operador lê o contêiner, confere o resumo e confirma. Se a conexão cair antes da confirmação, o comando entra na fila local. Após a reconexão, o backend valida novamente a ordem; se outro usuário já a concluiu, o comando muda para conflito em vez de duplicar a movimentação.
