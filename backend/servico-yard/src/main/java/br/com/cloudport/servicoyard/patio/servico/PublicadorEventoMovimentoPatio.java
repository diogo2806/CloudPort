package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.custodia.dto.CustodiaExchangeAreaRespostaDto;
import br.com.cloudport.servicoyard.patio.custodia.modelo.StatusCustodiaExchangeArea;
import br.com.cloudport.servicoyard.patio.dto.EventoMovimentoPatioDto;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventoMovimentoPatio {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoMovimentoPatio.class);
    private static final String EVENTO_MOVIMENTO = "yard.movimento.registrado";
    private static final String EVENTO_CUSTODIA_ENTREGUE = "yard.custodia.exchange-area.entregue";
    private static final String EVENTO_CUSTODIA_RECEBIDA = "yard.custodia.exchange-area.recebida";
    private static final String EVENTO_CUSTODIA_DIVERGENTE = "yard.custodia.exchange-area.divergente";
    private static final int VERSAO_EVENTO = 1;

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final boolean rabbitEnabled;

    @Autowired
    public PublicadorEventoMovimentoPatio(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                                          @Value("${cloudport.yard.eventos.exchange}") String exchange,
                                          @Value("${cloudport.yard.eventos.routing-movimento}") String routingKey,
                                          @Value("${cloudport.messaging.rabbit.enabled:false}") boolean rabbitEnabled) {
        this(rabbitTemplateProvider.getIfAvailable(), exchange, routingKey, rabbitEnabled);
    }

    public PublicadorEventoMovimentoPatio(RabbitTemplate rabbitTemplate,
                                          String exchange,
                                          String routingKey,
                                          boolean rabbitEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.rabbitEnabled = rabbitEnabled;
    }

    public void publicar(EventoMovimentoPatioDto evento) {
        publicar(EVENTO_MOVIMENTO, evento);
    }

    public void entregarNaExchangeArea(CustodiaExchangeAreaRespostaDto custodia) {
        publicar(EVENTO_CUSTODIA_ENTREGUE, custodia);
    }

    public void receberDaExchangeArea(CustodiaExchangeAreaRespostaDto custodia) {
        String tipoEvento = custodia.getStatus() == StatusCustodiaExchangeArea.DIVERGENTE
                ? EVENTO_CUSTODIA_DIVERGENTE
                : EVENTO_CUSTODIA_RECEBIDA;
        publicar(tipoEvento, custodia);
    }

    private void publicar(String tipoEvento, Object dados) {
        if (!rabbitEnabled) {
            LOGGER.debug("Evento {} não publicado porque o RabbitMQ está desabilitado.", tipoEvento);
            return;
        }
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitMQ habilitado sem RabbitTemplate disponível");
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", tipoEvento);
        envelope.put("eventVersion", VERSAO_EVENTO);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "servico-yard");
        envelope.put("data", dados);
        rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
    }
}
