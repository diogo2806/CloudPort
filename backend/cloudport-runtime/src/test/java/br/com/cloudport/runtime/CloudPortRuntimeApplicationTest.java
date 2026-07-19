package br.com.cloudport.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CloudPortRuntimeApplicationTest {

    @Test
    void deveAplicarConfiguracaoDoRabbitAosConsumidores() {
        CloudPortRuntimeApplication.criarAplicacao();

        String rabbitHabilitado = System.getProperty("cloudport.messaging.rabbit.enabled");
        String consumidoresHabilitados = System.getProperty("cloudport.runtime.consumers-enabled");

        assertNotNull(rabbitHabilitado);
        assertEquals(rabbitHabilitado, consumidoresHabilitados);
    }
}
