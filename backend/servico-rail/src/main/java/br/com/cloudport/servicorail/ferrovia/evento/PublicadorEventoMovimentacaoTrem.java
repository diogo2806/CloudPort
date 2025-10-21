package br.com.cloudport.servicorail.ferrovia.evento;

import br.com.cloudport.servicorail.ferrovia.dto.EventoMovimentacaoTremConcluidaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class PublicadorEventoMovimentacaoTrem {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoMovimentacaoTrem.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PublicadorEventoMovimentacaoTrem(RabbitTemplate rabbitTemplate,
                                            @Value("${cloudport.rail.eventos.exchange}") String exchange,
                                            @Value("${cloudport.rail.eventos.routing-movimentacao}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(EventoMovimentacaoTremConcluidaDto evento) {
        Assert.notNull(evento, "Evento da movimentação do trem não pode ser nulo");
        LOGGER.info("event=movimentacao_trem.publicada ordem={} visita={} conteiner={} tipo={}",
                evento.getIdOrdemMovimentacao(), evento.getIdVisitaTrem(), evento.getCodigoConteiner(),
                evento.getTipoMovimentacao());
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
    }
}
