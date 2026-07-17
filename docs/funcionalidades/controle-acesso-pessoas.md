# Controle de entrada e saída de pessoas

O módulo Gate mantém o estado atual e o histórico auditável das pessoas que entram e saem do terminal.

## Fluxo operacional

1. O operador informa nome, documento, tipo de vínculo, empresa, crachá, ponto de acesso e motivo.
2. A entrada coloca a pessoa na situação `DENTRO` e cria uma movimentação `ENTRADA`.
3. A pessoa aparece imediatamente na lista de presentes, com horário e tempo de permanência.
4. A saída é registrada pelo documento, fecha a presença e grava uma movimentação `SAIDA` com o tempo total no terminal.
5. O histórico pode ser filtrado por documento e contém operador responsável, origem da ação e correlation ID.

## Regras

- não é permitida uma segunda entrada enquanto a pessoa estiver dentro do terminal;
- não é permitida saída sem entrada aberta;
- documento é normalizado para evitar duplicidade com pontuação ou diferença entre letras maiúsculas e minúsculas;
- entrada e saída são transacionais e usam bloqueio pessimista por documento;
- somente `ADMIN_PORTO` e `OPERADOR_GATE` registram movimentações;
- `PLANEJADOR` possui acesso somente às consultas;
- o histórico retorna no máximo 200 registros por chamada.

## API

- `POST /gate/pessoas/entradas`
- `POST /gate/pessoas/saidas`
- `GET /gate/pessoas/presentes`
- `GET /gate/pessoas/resumo`
- `GET /gate/pessoas/movimentacoes?documento={documento}&limite={limite}`

## Portal

A tela está disponível em `Gate > Controle de Pessoas`, rota `/home/gate/pessoas`.
