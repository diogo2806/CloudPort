package br.com.cloudport.visibilidade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PORT_EVENTS_EXCHANGE = "port.events";

    // Queues
    public static final String VISIBILIDADE_GATE_QUEUE = "visibilidade.gate.events";
    public static final String VISIBILIDADE_YARD_QUEUE = "visibilidade.yard.events";
    public static final String VISIBILIDADE_NAVIO_QUEUE = "visibilidade.navio.events";
    public static final String VISIBILIDADE_RAIL_QUEUE = "visibilidade.rail.events";

    @Bean
    public TopicExchange portEventsExchange() {
        return new TopicExchange(PORT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue visibilidadeGateQueue() {
        return new Queue(VISIBILIDADE_GATE_QUEUE, true);
    }

    @Bean
    public Queue visibilidadeYardQueue() {
        return new Queue(VISIBILIDADE_YARD_QUEUE, true);
    }

    @Bean
    public Queue visibilidadeNavioQueue() {
        return new Queue(VISIBILIDADE_NAVIO_QUEUE, true);
    }

    @Bean
    public Queue visibilidadeRailQueue() {
        return new Queue(VISIBILIDADE_RAIL_QUEUE, true);
    }

    @Bean
    public Binding gateBinding(Queue visibilidadeGateQueue, TopicExchange portEventsExchange) {
        return BindingBuilder.bind(visibilidadeGateQueue).to(portEventsExchange).with("gate.*");
    }

    @Bean
    public Binding yardBinding(Queue visibilidadeYardQueue, TopicExchange portEventsExchange) {
        return BindingBuilder.bind(visibilidadeYardQueue).to(portEventsExchange).with("yard.*");
    }

    @Bean
    public Binding navioBinding(Queue visibilidadeNavioQueue, TopicExchange portEventsExchange) {
        return BindingBuilder.bind(visibilidadeNavioQueue).to(portEventsExchange).with("navio.*");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}