# Cockpit operacional por perfil

## Finalidade

O cockpit é a página inicial de decisão do CloudPort. Ele reúne exceções, filas, ordens, escalas, disponibilidade de recursos e falhas de integração relevantes aos papéis do usuário autenticado.

O painel não substitui os módulos operacionais. Cada cartão resume uma fonte e direciona para a lista correspondente, onde a análise e as ações são executadas.

## Fluxo operacional

1. Abra o painel inicial.
2. Confira **Prioridades detectadas**, **Fontes indisponíveis** e **Atualização**.
3. Priorize cartões marcados como **Requer atenção**.
4. Analise o valor principal, a explicação, a janela de dados, a tendência e a distribuição por estado.
5. Selecione **Abrir lista correspondente** para continuar no módulo.
6. Use **Atualizar agora** quando precisar de uma nova leitura imediata.
7. Configure a atualização automática conforme a necessidade operacional.
8. Use **Personalizar** para alterar a ordem ou ocultar cartões permitidos.

## Blocos disponíveis

### Exceções críticas

Origem: dashboard de visibilidade.

Mostra alertas ativos, críticos, altos ou ainda não reconhecidos conforme os campos retornados pelo runtime.

### Filas e SLA do Gate

Origem: visão completa dos agendamentos do Gate.

Considera veículos agendados, em fila, aguardando, chamados ou no pré-gate. Registros com atraso superior a 30 minutos ou estado de atraso, vencimento, retenção ou bloqueio são destacados.

### Trabalho do pátio

Origem: ordens do pátio.

Resume ordens bloqueadas, suspensas, com erro, falha ou purgatório, além de ordens pendentes, planejadas, criadas ou disponíveis.

### Operação ferroviária

Origem: visitas ferroviárias dos próximos 30 dias.

Mostra visitas ainda não encerradas, canceladas ou partidas. Destaca composições recebidas, em operação ou processamento.

### Navios e escalas

Origem: escalas de embarque dos próximos 30 dias.

Mostra escalas ainda não finalizadas, canceladas ou partidas e identifica navios atracados ou em operação.

### Disponibilidade de equipamentos

Origem: telemetria dos equipamentos do pátio.

Conta equipamentos offline, indisponíveis, em falha, manutenção, parados, sem sinal ou explicitamente marcados como não conectados, indisponíveis ou não prontos.

### Integrações EDI

Origem: últimos 50 processamentos BAPLIE.

Destaca processamentos com erro, falha, rejeição, invalidade ou reprocessamento pendente.

## Campos e cálculos

- **Prioridades detectadas:** soma das ocorrências de atenção calculadas nos cartões permitidos.
- **Blocos permitidos:** quantidade de fontes liberadas pelos papéis atuais.
- **Fontes indisponíveis:** quantidade de chamadas que falharam na atualização atual.
- **Atualização:** data e hora da última leitura consolidada.
- **Valor principal:** quantidade calculada para o cartão.
- **Descrição do valor:** detalha como a quantidade foi composta.
- **Período:** janela temporal ou universo consultado.
- **Tendência:** diferença entre a leitura atual e a leitura anterior salva para o mesmo usuário.
- **Distribuição por estado:** agrupamento dos registros pelo estado, fase ou situação retornada.
- **Tabela equivalente:** representação textual dos mesmos valores exibidos nas barras.
- **Atualização automática:** intervalo de 30, 60, 120 ou 300 segundos; também pode ser desativada.

A tendência não representa previsão estatística. Ela compara somente duas leituras consecutivas do navegador atual.

## Permissões

| Bloco | Papéis |
| --- | --- |
| Exceções críticas | usuário autenticado com acesso ao painel |
| Gate | `ADMIN_PORTO`, `OPERADOR_GATE`, `PLANEJADOR` |
| Pátio | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO` |
| Ferrovia | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO` |
| Navios | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_GATE` |
| Equipamentos | `ADMIN_PORTO`, `PLANEJADOR`, `OPERADOR_PATIO`, `OPERADOR_GATE` |
| EDI | `ADMIN_PORTO`, `PLANEJADOR` |

O frontend não chama fontes fora do perfil. As APIs mantêm a validação definitiva de autorização.

## Estados

- **Carregando:** primeira leitura em andamento.
- **Disponível:** a fonte respondeu sem ocorrências classificadas como atenção.
- **Requer atenção:** existem bloqueios, atrasos, falhas ou indisponibilidades.
- **Sem pendências:** a fonte respondeu, mas não retornou registros relevantes.
- **Indisponível:** somente aquela fonte falhou.
- **Dados desatualizados:** a última leitura ultrapassou duas vezes o intervalo esperado, com mínimo de 60 segundos.

## Falhas parciais

As fontes são carregadas com isolamento. Uma falha de EDI não impede Gate, Pátio ou Ferrovia de aparecerem. O cartão indisponível mostra a mensagem sanitizada e oferece **Tentar novamente**.

O aviso geral aparece somente quando todas as fontes permitidas falham.

## Personalização

Selecione **Personalizar** para:

- mover um cartão para cima ou para baixo;
- ocultar um cartão;
- reativar cartões ocultos;
- restaurar o padrão do perfil.

A preferência é armazenada por usuário no navegador. Uma mudança de permissão remove automaticamente cartões não autorizados da composição efetiva.

## Motivos de bloqueio

- sessão expirada;
- API indisponível ou excedendo o tempo limite;
- perfil sem acesso ao módulo;
- integração de origem sem resposta;
- armazenamento local indisponível;
- todos os cartões ocultos;
- resposta sem estado reconhecido, agrupada como `SEM_STATUS`;
- atualização automática desativada, exigindo ação manual.

## Exemplo por perfil

Um usuário com `OPERADOR_PATIO` recebe Exceções críticas, Trabalho do pátio, Operação ferroviária e Disponibilidade de equipamentos. O frontend não consulta Gate, Navios ou EDI quando esses blocos não pertencem ao papel.

Se o pátio possuir duas ordens bloqueadas e cinco pendentes, o cartão exibe sete itens no valor principal, informa duas ordens bloqueadas ou suspensas e apresenta a distribuição dos estados. O operador abre a lista de trabalho para tratar os registros.

## Atalhos

- `F1`: abre o manual contextual do cockpit.
- `Shift + ?`: abre o manual fora de campos de edição.
- `Ctrl + K` ou `Command + K`: abre a busca global de telas.
- `Tab` e `Shift + Tab`: percorrem filtros, cartões e ações.
- `Enter` ou `Espaço`: executa a ação em foco.

## Acessibilidade

- estados são escritos em texto e não dependem somente de cor;
- tendências possuem símbolo e descrição;
- barras possuem rótulos, números e tabela equivalente;
- botões de organização possuem nomes acessíveis;
- o botão **Personalizar** informa estado pressionado;
- a grade se adapta a desktop, tablet e celular;
- movimento reduzido é respeitado.

## Processo completo

Ao abrir a página, o portal determina os cartões permitidos, recupera a personalização do usuário, chama somente as fontes necessárias e monta cada cartão de forma independente. Depois da leitura, salva uma fotografia dos valores para calcular a tendência na atualização seguinte. O usuário pode abrir a lista operacional, reorganizar a página, ocultar fontes permitidas ou alterar o intervalo de atualização sem afetar outros usuários.