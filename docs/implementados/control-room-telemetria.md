# Control Room de equipamentos e telemetria

## Escopo entregue

O CloudPort possui um Control Room próprio para equipamentos do pátio, separado do painel incorporado de Navio + Pátio. A entrega cobre:

- visão operacional dos equipamentos e seus estados;
- telemetria atual e histórico de leituras;
- atualização quase em tempo real por Server-Sent Events;
- alarmes técnicos de telemetria atrasada, dispositivo desconectado, falha de dispositivo e indisponibilidade;
- reconhecimento e resolução de alarmes;
- abertura e encerramento de indisponibilidades;
- criação, entrega e confirmação de comandos remotos;
- heartbeat e cadastro automático dos dispositivos integrados;
- controle por perfil e trilha operacional de comandos.

## Fluxo de integração do dispositivo

1. O dispositivo envia heartbeat para `POST /yard/control-room/dispositivos/{dispositivo}/heartbeat`.
2. A telemetria de posição continua sendo recebida em `POST /yard/patio/equipamentos/telemetria/{equipamento}`.
3. O Control Room persiste a posição atual e uma cópia histórica de cada leitura.
4. O dispositivo consulta `GET /yard/control-room/dispositivos/{dispositivo}/comandos-pendentes`.
5. Depois de executar o comando, confirma em `POST /yard/control-room/dispositivos/{dispositivo}/comandos/{comandoId}/confirmacao`.
6. Mudanças relevantes são publicadas no canal `GET /yard/control-room/stream`.

As rotas de dispositivo aceitam autenticação interna pelo cabeçalho `X-CloudPort-Service-Key`. As rotas do portal usam JWT.

## Comandos suportados

- `DISPONIBILIZAR`
- `INDISPONIBILIZAR`
- `ENVIAR_MENSAGEM`
- `MOVER_PARA_POSICAO`
- `SINCRONIZAR_TELEMETRIA`
- `RESETAR_POSICAO`

Os comandos percorrem os estados `PENDENTE`, `ENVIADO`, `EXECUTADO`, `FALHOU` ou `CANCELADO`.

## Saúde operacional

A avaliação automática considera:

- telemetria atrasada após dois minutos sem nova leitura;
- dispositivo desconectado após noventa segundos sem heartbeat;
- estados de falha recebidos pelo VMT;
- indisponibilidade aberta manualmente ou por comando remoto.

A avaliação é executada periodicamente e também antes das consultas principais do painel.

## Perfis

Consulta:

- `ROLE_ADMIN_PORTO`
- `ROLE_PLANEJADOR`
- `ROLE_OPERADOR_PATIO`
- `ROLE_OPERADOR_GATE`

Comandos e alterações operacionais:

- `ROLE_ADMIN_PORTO`
- `ROLE_PLANEJADOR`
- `ROLE_OPERADOR_PATIO`

Integração de dispositivos:

- `ROLE_SERVICE_NAVIO`
- `ROLE_ADMIN_PORTO`

## Interface

A tela está disponível em `/home/control-room` e apresenta:

- indicadores operacionais;
- filtros por status, tipo e conectividade;
- detalhes do equipamento;
- envio de comandos;
- histórico de telemetria;
- gestão de indisponibilidades;
- alarmes técnicos;
- comandos recentes;
- dispositivos conectados;
- estado do canal em tempo real.
