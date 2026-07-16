package br.com.cloudport.visibilidade.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitMQConfigTest {

    @Test
    void deveConectarYardERailAosExchangesQuePublicamOsEventos() {
        RabbitMQConfig config = new RabbitMQConfig(
                "port.events",
                "yard.eventos",
                "yard.movimento.registrado",
                "ferrovia.eventos",
                "rail.movimentacao.concluida");

        Queue yardQueue = config.visibilidadeYardQueue();
        TopicExchange yardExchange = config.eventosYardExchange();
        Binding yardBinding = config.yardBinding(yardQueue, yardExchange);
        assertEquals("yard.eventos", yardBinding.getExchange());
        assertEquals("yard.movimento.registrado", yardBinding.getRoutingKey());

        Queue railQueue = config.visibilidadeRailQueue();
        TopicExchange railExchange = config.eventosRailExchange();
        Binding railBinding = config.railBinding(railQueue, railExchange);
        assertEquals("ferrovia.eventos", railBinding.getExchange());
        assertEquals("rail.movimentacao.concluida", railBinding.getRoutingKey());
    }

    @Test
    void deveAceitarEventosComMaisDeUmSegmentoNoExchangeLegado() {
        RabbitMQConfig config = new RabbitMQConfig(
                "port.events",
                "yard.eventos",
                "yard.movimento.registrado",
                "ferrovia.eventos",
                "rail.movimentacao.concluida");
        TopicExchange legado = config.eventosLegadoExchange();

        assertEquals("gate.#", config.gateBinding(config.visibilidadeGateQueue(), legado).getRoutingKey());
        assertEquals("navio.#", config.navioBinding(config.visibilidadeNavioQueue(), legado).getRoutingKey());
        assertEquals("yard.#", config.yardLegadoBinding(config.visibilidadeYardQueue(), legado).getRoutingKey());
        assertEquals("rail.#", config.railLegadoBinding(config.visibilidadeRailQueue(), legado).getRoutingKey());
    }
}
