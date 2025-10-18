package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.EventoMovimentoPatioDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicadorEventoMovimentoPatio {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PublicadorEventoMovimentoPatio(RabbitTemplate rabbitTemplate,
                                          @Value("${cloudport.yard.eventos.exchange}") String exchange,
                                          @Value("${cloudport.yard.eventos.routing-movimento}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publicar(EventoMovimentoPatioDto evento) {
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
    }
}
