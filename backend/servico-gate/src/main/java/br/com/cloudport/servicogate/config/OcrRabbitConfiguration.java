package br.com.cloudport.servicogate.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OcrIntegrationProperties.class)
public class OcrRabbitConfiguration {

    private final OcrIntegrationProperties properties;

    public OcrRabbitConfiguration(OcrIntegrationProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Declarables ocrDeclarables() {
        return new Declarables(
                QueueBuilder.durable(properties.getSolicitacaoQueue()).build()
        );
    }
}
