package br.com.cloudport.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;

class CloudPortRuntimeApplicationTest {

    @Test
    void deveAplicarConfiguracaoDoRabbitAosConsumidores() {
        CloudPortRuntimeApplication.criarAplicacao();

        String rabbitHabilitado = System.getProperty("cloudport.messaging.rabbit.enabled");
        String consumidoresHabilitados = System.getProperty("cloudport.runtime.consumers-enabled");

        assertNotNull(rabbitHabilitado);
        assertEquals(rabbitHabilitado, consumidoresHabilitados);

        if (!Boolean.parseBoolean(rabbitHabilitado)) {
            String exclusoes = System.getProperty("spring.autoconfigure.exclude");
            assertNotNull(exclusoes);
            assertTrue(exclusoes.contains(RabbitAutoConfiguration.class.getName()));
        }
    }
}
