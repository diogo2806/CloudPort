# Integração de barcode com DMT

## Estado atual

A integração com dispositivo DMT não está ativa no CloudPort porque ainda não existe cliente externo implementado, contratado e validado para consumir solicitações de leitura.

O módulo Gate não publica mensagens RabbitMQ para DMT e não depende de broker para iniciar ou operar. A propriedade `GATE_BARCODE_HABILITADO` permanece `false` por padrão, e a aplicação impede sua ativação enquanto não existir um adaptador real.

Os endpoints de confirmação podem permanecer no código como preparação de contrato, mas não representam uma integração operacional completa sem o cliente que inicia e responde ao fluxo.

## Decisão arquitetural

Mensageria somente poderá ser introduzida quando houver simultaneamente:

1. consumidor externo real identificado;
2. contrato de mensagem versionado;
3. necessidade comprovada de entrega assíncrona;
4. política de retry, idempotência e dead-letter;
5. monitoramento e responsabilidade operacional definidos;
6. testes ponta a ponta com o dispositivo ou sistema consumidor.

Até esses critérios serem atendidos, o Gate deve usar chamadas locais para processamento interno e HTTP apenas para integrações externas existentes.

## OCR

A validação básica de imagem OCR é executada localmente no mesmo processo do Gate. Não existe fila entre o upload do documento e o executor de validação.

## Configuração permitida

```properties
gate.barcode.habilitado=${GATE_BARCODE_HABILITADO:false}
cloudport.gate.ocr.tempo-maximo-processamento=${GATE_OCR_TIMEOUT:PT5S}
```

Não devem ser configuradas variáveis de RabbitMQ para o módulo Gate.
