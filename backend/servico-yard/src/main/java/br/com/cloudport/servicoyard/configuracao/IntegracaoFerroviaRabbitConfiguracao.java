package br.com.cloudport.servicoyard.configuracao;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegracaoFerroviaRabbitConfiguracao {

    @Bean
    public TopicExchange exchangeMovimentacaoFerrovia(
            @Value("${cloudport.yard.integracoes.ferrovia.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaMovimentacaoFerrovia(
            @Value("${cloudport.yard.integracoes.ferrovia.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding bindingMovimentacaoFerrovia(Queue filaMovimentacaoFerrovia,
                                               TopicExchange exchangeMovimentacaoFerrovia,
                                               @Value("${cloudport.yard.integracoes.ferrovia.routing}") String routingKey) {
        return BindingBuilder.bind(filaMovimentacaoFerrovia)
                .to(exchangeMovimentacaoFerrovia)
                .with(routingKey);
    }
}
