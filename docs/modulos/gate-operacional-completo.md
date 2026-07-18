# Gate operacional completo

## Escopo

O módulo consolida a operação rodoviária do terminal em torno de `truck_visit`, permitindo uma visita com uma ou mais transações e execução por estágios configuráveis.

A implementação cobre:

- Bills of Lading;
- bookings;
- EDO, ERO e IDO;
- pré-aviso de exportação e de vazio;
- facilities, Gates e pistas;
- estágios, transições e business tasks configuráveis;
- appointments e consumo atômico da capacidade das janelas;
- truck visit com múltiplas transações;
- trouble transactions;
- inspeções;
- fotografias, avarias e documentos externos;
- ticket e EIR com controle de reimpressão;
- transferências entre instalações;
- lane monitor;
- regras de bloqueio e permissão para motorista, transportadora e veículo;
- histórico auditável das mudanças de estágio.

## Modelo operacional

### Referências

`gate_booking`, `gate_bill_of_lading`, `gate_order` e `gate_preadvice` formam o conjunto de referências aceitas no pré-check. Uma ordem pode ser vinculada a um Bill of Lading. Ao criar a transação, o banco herda o BL da ordem e valida vigência, status e saldo.

Quando a transação é concluída, o saldo do booking e do BL é consumido de forma transacional. EDO, ERO, IDO e pré-aviso passam para o estado utilizado.

### Appointments e capacidade

A abertura da truck visit para um agendamento:

1. garante que o agendamento pertence à transportadora e ao veículo;
2. impede uma segunda truck visit para o mesmo agendamento;
3. incrementa a capacidade utilizada da janela sob bloqueio transacional;
4. rejeita a operação quando a janela está lotada;
5. atualiza o agendamento para `EM_EXECUCAO`.

### Estágios e business tasks

Cada Gate possui estágios ordenados, transições permitidas e business tasks. O avanço somente ocorre quando todas as tarefas obrigatórias do estágio atual são informadas como concluídas e não existe trouble aberto.

A configuração inicial contém:

1. Pré-check;
2. OCR e identificação;
3. Balança;
4. Inspeção;
5. Liberação;
6. Conclusão.

### Trouble e inspeção

Uma trouble transaction retém a transação e a truck visit. O avanço volta a ser permitido somente depois da resolução de todos os troubles abertos. Uma inspeção reprovada abre trouble de severidade alta automaticamente.

### Regras de acesso

As regras podem operar como `BLOQUEIO` ou `PERMISSAO` e são aplicadas a motorista, transportadora ou veículo.

- um bloqueio vigente impede imediatamente a entrada;
- quando há lista de permissão vigente para determinado escopo, apenas referências explicitamente permitidas podem entrar;
- a validação ocorre no banco antes da inserção da truck visit, evitando bypass por outro cliente da API.

### Documentos e anexos

A operação suporta anexos categorizados como fotografia, documento, avaria, OCR ou inspeção. Tickets, EIRs e comprovantes de transferência são emitidos com numeração única e contador de reimpressões.

### Múltiplas instalações

A truck visit pode ser transferida para outra facility. No recebimento, o sistema seleciona o primeiro Gate e estágio ativos do destino, reposiciona todas as transações e reinicia o processamento.

## API

Base: `/gate/operacional`

Principais recursos:

- `GET /painel`;
- `GET /referencias`;
- `GET /complementos`;
- `POST /configuracao/facilities`;
- `POST /configuracao/gates`;
- `POST /configuracao/lanes`;
- `POST /configuracao/stages`;
- `POST /configuracao/stages/{stageId}/tasks`;
- `POST /configuracao/regras-acesso`;
- `POST /bookings`;
- `POST /bills-of-lading`;
- `POST /ordens`;
- `POST /ordens/{ordemId}/bill-of-lading/{billOfLadingId}`;
- `POST /pre-avisos`;
- `POST /visitas`;
- `POST /visitas/{visitaId}/avancar`;
- `POST /transacoes/{transactionId}/troubles`;
- `POST /troubles/{troubleId}/resolver`;
- `POST /transacoes/{transactionId}/inspecoes`;
- `POST /anexos`;
- `POST /visitas/{visitaId}/documentos`;
- `POST /documentos/{documentoId}/reimprimir`;
- `POST /visitas/{visitaId}/transferencias`;
- `POST /transferencias/{transferenciaId}/receber`.

## Interface

A rota `/home/gate/operacao` apresenta:

- indicadores de visitas, troubles, capacidade e referências;
- lane monitor com ocupação e recursos de OCR, balança e inspeção;
- fluxo visual dos estágios;
- tabela operacional de truck visits;
- inspector com business tasks e ações por transação;
- emissão de ticket e EIR;
- transferência entre facilities;
- painéis de troubles, Bills of Lading e regras de acesso.

## Segurança

- consulta operacional: `ADMIN_PORTO`, `PLANEJADOR` e `OPERADOR_GATE`;
- execução de estágio, trouble, inspeção e documentos: `ADMIN_PORTO` e `OPERADOR_GATE`;
- configuração estrutural e regras de acesso: `ADMIN_PORTO`;
- manutenção de referências: `ADMIN_PORTO` e `PLANEJADOR`;
- pré-aviso também pode ser criado por `TRANSPORTADORA`.
