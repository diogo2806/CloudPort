package br.com.cloudport.visibilidade.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "cloudport.messaging.rabbit.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RabbitMQConfig {

    public static final String VISIBILIDADE_GATE_QUEUE = "visibilidade.gate.events";
    public static final String VISIBILIDADE_YARD_QUEUE = "visibilidade.yard.events";
    public static final String VISIBILIDADE_NAVIO_QUEUE = "visibilidade.navio.events";
    public static final String VISIBILIDADE_RAIL_QUEUE = "visibilidade.rail.events";
    public static final String VISIBILIDADE_RAIL_REJEITADOS_QUEUE = "visibilidade.rail.rejeitados";

    private final String exchangeLegado;
    private final String exchangeYard;
    private final String routingYard;
    private final String exchangeRail;
    private final String routingRail;
    private final String exchangeRailRejeitados;
    private final String queueRailRejeitados;
    private final String routingRailRejeitados;
    private final boolean rabbitEnabled;

    public RabbitMQConfig(String exchangeLegado,
                          String exchangeYard,
                          String routingYard,
                          String exchangeRail,
                          String routingRail) {
        this(exchangeLegado, exchangeYard, routingYard, exchangeRail, routingRail,
                "visibilidade.rail.rejeitados", VISIBILIDADE_RAIL_REJEITADOS_QUEUE, "rail.rejeitado", false);
    }

    @Autowired
    public RabbitMQConfig(
            @Value("${cloudport.visibilidade.eventos.legado.exchange:port.events}") String exchangeLegado,
            @Value("${cloudport.visibilidade.eventos.yard.exchange:yard.eventos}") String exchangeYard,
            @Value("${cloudport.visibilidade.eventos.yard.routing:yard.movimento.registrado}") String routingYard,
            @Value("${cloudport.visibilidade.eventos.rail.exchange:ferrovia.eventos}") String exchangeRail,
            @Value("${cloudport.visibilidade.eventos.rail.routing:rail.movimentacao.concluida}") String routingRail,
            @Value("${cloudport.visibilidade.eventos.rail.rejeitados.exchange:visibilidade.rail.rejeitados}") String exchangeRailRejeitados,
            @Value("${cloudport.visibilidade.eventos.rail.rejeitados.queue:visibilidade.rail.rejeitados}") String queueRailRejeitados,
            @Value("${cloudport.visibilidade.eventos.rail.rejeitados.routing:rail.rejeitado}") String routingRailRejeitados,
            @Value("${cloudport.messaging.rabbit.enabled:false}") boolean rabbitEnabled) {
        this.exchangeLegado = exchangeLegado;
        this.exchangeYard = exchangeYard;
        this.routingYard = routingYard;
        this.exchangeRail = exchangeRail;
        this.routingRail = routingRail;
        this.exchangeRailRejeitados = exchangeRailRejeitados;
        this.queueRailRejeitados = queueRailRejeitados;
        this.routingRailRejeitados = routingRailRejeitados;
        this.rabbitEnabled = rabbitEnabled;
    }

    @Bean("eventosLegadoExchange")
    public TopicExchange eventosLegadoExchange() {
        return new TopicExchange(exchangeLegado, true, false);
    }

    @Bean("eventosYardExchange")
    public TopicExchange eventosYardExchange() {
        return new TopicExchange(exchangeYard, true, false);
    }

    @Bean("eventosRailExchange")
    public TopicExchange eventosRailExchange() {
        return new TopicExchange(exchangeRail, true, false);
    }

    @Bean("eventosRailRejeitadosExchange")
    public TopicExchange eventosRailRejeitadosExchange() {
        return new TopicExchange(exchangeRailRejeitados, true, false);
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
    public Queue visibilidadeRailRejeitadosQueue() {
        return new Queue(queueRailRejeitados, true);
    }

    @Bean
    public Binding gateBinding(@Qualifier("visibilidadeGateQueue") Queue queue,
                               @Qualifier("eventosLegadoExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("gate.#");
    }

    @Bean
    public Binding navioBinding(@Qualifier("visibilidadeNavioQueue") Queue queue,
                                @Qualifier("eventosLegadoExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("navio.#");
    }

    @Bean
    public Binding yardBinding(@Qualifier("visibilidadeYardQueue") Queue queue,
                               @Qualifier("eventosYardExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingYard);
    }

    @Bean
    public Binding yardLegadoBinding(@Qualifier("visibilidadeYardQueue") Queue queue,
                                     @Qualifier("eventosLegadoExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("yard.#");
    }

    @Bean
    public Binding railBinding(@Qualifier("visibilidadeRailQueue") Queue queue,
                               @Qualifier("eventosRailExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingRail);
    }

    @Bean
    public Binding railLegadoBinding(@Qualifier("visibilidadeRailQueue") Queue queue,
                                     @Qualifier("eventosLegadoExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("rail.#");
    }

    @Bean
    public Binding railRejeitadosBinding(
            @Qualifier("visibilidadeRailRejeitadosQueue") Queue queue,
            @Qualifier("eventosRailRejeitadosExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingRailRejeitados);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter,
            @Value("${spring.rabbitmq.listener.simple.concurrency:1}") int concurrentConsumers,
            @Value("${spring.rabbitmq.listener.simple.max-concurrency:3}") int maxConcurrentConsumers,
            @Value("${spring.rabbitmq.listener.simple.prefetch:10}") int prefetchCount) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setPrefetchCount(prefetchCount);
        factory.setDefaultRequeueRejected(true);
        factory.setAutoStartup(rabbitEnabled);
        return factory;
    }
}
