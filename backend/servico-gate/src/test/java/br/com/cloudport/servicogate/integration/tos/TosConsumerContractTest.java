package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicogate.contingencia.ContingenciaProperties;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = TosConsumerContractTest.TestConfig.class)
@AutoConfigureWireMock(port = 0, stubs = "classpath:/contracts/tos")
class TosConsumerContractTest {

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("cloudport.tos.api.base-url", () -> "http://localhost:" + Integer.getInteger("wiremock.server.port", 0));
    }

    @Autowired
    private TosIntegrationService integrationService;

    @Test
    @DisplayName("Contrato: deve mapear booking liberado vindo do TOS")
    void deveRespeitarContratoBookingLiberado() {
        var info = integrationService.obterBookingInfo("BK999");

        assertThat(info).isNotNull();
        assertThat(info.getBookingNumber()).isEqualTo("BK999");
        assertThat(info.isLiberado()).isTrue();
        assertThat(info.getVessel()).isEqualTo("Cloud Express");
    }

    @TestConfiguration
    @Import(TosClientConfig.class)
    static class TestConfig {

        @Bean
        TosProperties tosProperties() {
            TosProperties properties = new TosProperties();
            properties.getApi().setBaseUrl("http://localhost:" + Integer.getInteger("wiremock.server.port", 0));
            return properties;
        }

        @Bean
        IntegracaoDegradacaoHandler integracaoDegradacaoHandler() {
            return new IntegracaoDegradacaoHandler(new GateMetrics(new SimpleMeterRegistry()));
        }

        @Bean
        ContingenciaProperties contingenciaProperties() {
            return new ContingenciaProperties();
        }

        @Bean
        TosClient tosClient(TosClientConfig config,
                            TosProperties properties,
                            IntegracaoDegradacaoHandler degradacaoHandler,
                            ContingenciaProperties contingenciaProperties) {
            return new TosClient(
                    config.tosWebClient(properties),
                    properties,
                    config.tosRetry(properties),
                    config.tosCircuitBreaker(CircuitBreakerRegistry.ofDefaults()),
                    degradacaoHandler,
                    contingenciaProperties
            );
        }

        @Bean
        TosIntegrationService tosIntegrationService(TosClient client,
                                                     TosClientConfig config,
                                                     TosProperties properties) {
            return new TosIntegrationService(
                    client,
                    new TosResponseAdapter(),
                    config.cacheManager(properties)
            );
        }
    }
}

