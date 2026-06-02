package br.com.cloudport.servicogate.integration.dmt;

import br.com.cloudport.servicogate.integration.dmt.DmtBarcodeService.BarcodeConfirmacaoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqDmtPublisher implements DmtMessagePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDmtPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${dmt.rabbitmq.exchange:dmt-barcode-exchange}")
    private String exchange;

    @Value("${dmt.rabbitmq.routing-key:barcode.confirmacao.solicitacao}")
    private String routingKey;

    public RabbitMqDmtPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void enviarSolicitacaoBarcode(BarcodeConfirmacaoRequest request) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, request);
            LOGGER.info("event=dmt.barcode.enviado exchange={} routingKey={} token={}",
                    exchange, routingKey, request.getTokenGatePass());
        } catch (Exception ex) {
            LOGGER.error("event=dmt.barcode.envio.erro token={} causa={}",
                    request.getTokenGatePass(), ex.getMessage(), ex);
        }
    }
}
