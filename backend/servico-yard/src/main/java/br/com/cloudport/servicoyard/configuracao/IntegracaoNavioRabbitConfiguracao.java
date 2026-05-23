package br.com.cloudport.servicoyard.configuracao;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegracaoNavioRabbitConfiguracao {

    @Bean
    public TopicExchange exchangeMovimentacaoNavio(
            @Value("${cloudport.yard.integracoes.navio.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaMovimentacaoNavio(
            @Value("${cloudport.yard.integracoes.navio.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding bindingMovimentacaoNavio(Queue filaMovimentacaoNavio,
                                            TopicExchange exchangeMovimentacaoNavio,
                                            @Value("${cloudport.yard.integracoes.navio.routing}") String routingKey) {
        return BindingBuilder.bind(filaMovimentacaoNavio)
                .to(exchangeMovimentacaoNavio)
                .with(routingKey);
    }
}
