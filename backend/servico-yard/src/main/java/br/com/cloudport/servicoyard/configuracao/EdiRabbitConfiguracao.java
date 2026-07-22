package br.com.cloudport.servicoyard.configuracao;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestrutura RabbitMQ para mensagens EDI (COPRAR e COARRI).
 *
 * Exchange: edi.mensagens  (routing-key: edi.coprar / edi.coarri)
 * Filas:
 *   edi.mensagens.coprar  – mudanças no plano de carga
 *   edi.mensagens.coarri  – confirmações de operação
 */
@Configuration
@ConditionalOnProperty(
        name = "cloudport.messaging.rabbit.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EdiRabbitConfiguracao {

    @Bean
    public TopicExchange exchangeEdi(
            @Value("${cloudport.yard.integracoes.edi.exchange:edi.mensagens}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaEdiCoprar(
            @Value("${cloudport.yard.integracoes.edi.queue.coprar:edi.mensagens.coprar}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Queue filaEdiCoarri(
            @Value("${cloudport.yard.integracoes.edi.queue.coarri:edi.mensagens.coarri}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding bindingCoprar(Queue filaEdiCoprar, TopicExchange exchangeEdi,
            @Value("${cloudport.yard.integracoes.edi.routing.coprar:edi.coprar}") String rk) {
        return BindingBuilder.bind(filaEdiCoprar).to(exchangeEdi).with(rk);
    }

    @Bean
    public Binding bindingCoarri(Queue filaEdiCoarri, TopicExchange exchangeEdi,
            @Value("${cloudport.yard.integracoes.edi.routing.coarri:edi.coarri}") String rk) {
        return BindingBuilder.bind(filaEdiCoarri).to(exchangeEdi).with(rk);
    }
}
