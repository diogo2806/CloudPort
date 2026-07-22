package br.com.cloudport.servicorail.ferrovia.evento;

import br.com.cloudport.servicorail.ferrovia.dto.EventoMovimentacaoTremConcluidaDto;
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
import org.springframework.util.Assert;

@Component
public class PublicadorEventoMovimentacaoTrem {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoMovimentacaoTrem.class);
    private static final String TIPO_EVENTO = "rail.movimentacao.concluida";
    private static final int VERSAO_EVENTO = 1;

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final boolean rabbitEnabled;

    @Autowired
    public PublicadorEventoMovimentacaoTrem(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                                            @Value("${cloudport.rail.eventos.exchange}") String exchange,
                                            @Value("${cloudport.rail.eventos.routing-movimentacao}") String routingKey,
                                            @Value("${cloudport.messaging.rabbit.enabled:false}") boolean rabbitEnabled) {
        this(rabbitTemplateProvider.getIfAvailable(), exchange, routingKey, rabbitEnabled);
    }

    public PublicadorEventoMovimentacaoTrem(RabbitTemplate rabbitTemplate,
                                            String exchange,
                                            String routingKey,
                                            boolean rabbitEnabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.rabbitEnabled = rabbitEnabled;
    }

    public void publicar(EventoMovimentacaoTremConcluidaDto evento) {
        Assert.notNull(evento, "Evento da movimentação do trem não pode ser nulo");
        if (!rabbitEnabled) {
            LOGGER.debug("Evento da movimentação do trem não publicado porque o RabbitMQ está desabilitado.");
            return;
        }
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitMQ habilitado sem RabbitTemplate disponível");
        }

        LOGGER.info("event=movimentacao_trem.publicada ordem={} visita={} conteiner={} tipo={}",
                evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem(), evento.getCodigoConteiner(),
                evento.getTipoMovimentacao());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", TIPO_EVENTO);
        envelope.put("eventVersion", VERSAO_EVENTO);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("source", "servico-rail");
        envelope.put("data", evento);
        rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
    }
}
