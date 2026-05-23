package br.com.cloudport.serviconavio.escala.evento;

import br.com.cloudport.serviconavio.escala.dto.EventoMovimentacaoNavioConcluidaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class PublicadorEventoMovimentacaoNavio {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoMovimentacaoNavio.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PublicadorEventoMovimentacaoNavio(RabbitTemplate rabbitTemplate,
                                             @Value("${cloudport.navio.eventos.exchange}") String exchange,
                                             @Value("${cloudport.navio.eventos.routing-movimentacao}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(EventoMovimentacaoNavioConcluidaDto evento) {
        Assert.notNull(evento, "Evento da movimentação do navio não pode ser nulo");
        LOGGER.info("event=movimentacao_navio.publicada ordem={} escala={} conteiner={} tipo={}",
                evento.getIdOrdemMovimentacao(), evento.getIdEscala(), evento.getCodigoConteiner(),
                evento.getTipoMovimentacao());
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
    }
}
