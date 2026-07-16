# Integração de barcode no fluxo de Gate

## Status

Esta fase não está implementada como integração operacional.

O código de domínio mantém estados e endpoints relacionados à confirmação de barcode, porém não existe cliente DMT real para receber uma solicitação e concluir o fluxo. Por esse motivo:

- `GATE_BARCODE_HABILITADO` deve permanecer `false`;
- o Gate não publica solicitações em RabbitMQ;
- o fluxo normal de entrada não fica aguardando uma resposta inexistente;
- a aplicação rejeita a ativação da funcionalidade sem um adaptador DMT real.

## Condição para implementação futura

A funcionalidade somente deve ser retomada após definição do cliente, protocolo, autenticação, idempotência, timeout, retry, observabilidade e teste ponta a ponta.

A escolha do transporte deve ser feita nessa etapa. RabbitMQ não é requisito prévio e não deve ser adicionado apenas para antecipar uma integração hipotética.

## Processamento interno

Operações internas do módulo Gate, como a validação básica de imagem OCR, são chamadas diretamente no mesmo processo. Não existe produtor e consumidor local separados por fila.
