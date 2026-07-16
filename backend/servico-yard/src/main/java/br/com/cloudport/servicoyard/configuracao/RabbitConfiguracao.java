package br.com.cloudport.servicoyard.configuracao;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguracao {

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter conversorMensagemJson() {
        return new Jackson2JsonMessageConverter();
    }
}
