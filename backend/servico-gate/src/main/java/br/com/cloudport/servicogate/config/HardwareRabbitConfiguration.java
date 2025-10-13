package br.com.cloudport.servicogate.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(HardwareIntegrationProperties.class)
public class HardwareRabbitConfiguration {

    private final HardwareIntegrationProperties properties;

    public HardwareRabbitConfiguration(HardwareIntegrationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Declarables gateHardwareDeclarables() {
        return new Declarables(
                QueueBuilder.durable(properties.getEntradaQueue()).build(),
                QueueBuilder.durable(properties.getSaidaQueue()).build(),
                ExchangeBuilder.directExchange(properties.getDecisaoExchange()).durable(true).build()
        );
    }
}
