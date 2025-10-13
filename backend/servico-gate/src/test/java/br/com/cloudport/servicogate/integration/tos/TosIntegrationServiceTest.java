package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.cloudport.servicogate.contingencia.ContingenciaProperties;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationException;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;

class TosIntegrationServiceTest {

    private MockWebServer mockWebServer;
    private TosIntegrationService integrationService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        TosProperties properties = new TosProperties();
        properties.getApi().setBaseUrl(mockWebServer.url("/").toString());
        properties.getApi().setTimeout(Duration.ofSeconds(5));
        properties.getCache().setMaxSize(10);
        properties.getCache().setTtl(Duration.ofSeconds(30));

        TosClientConfig config = new TosClientConfig();
        WebClient webClient = config.tosWebClient(properties);
        CacheManager cacheManager = config.cacheManager(properties);

        IntegracaoDegradacaoHandler degradacaoHandler = new IntegracaoDegradacaoHandler(
                new GateMetrics(new SimpleMeterRegistry()));
        TosClient client = new TosClient(
                webClient,
                properties,
                config.tosRetry(properties),
                config.tosCircuitBreaker(CircuitBreakerRegistry.ofDefaults()),
                degradacaoHandler,
                new ContingenciaProperties()
        );

        integrationService = new TosIntegrationService(
                client,
                new TosResponseAdapter(),
                cacheManager
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Deve validar criação de agendamento quando booking está liberado no TOS")
    void deveValidarAgendamentoQuandoBookingLiberado() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"bookingNumber\":\"BK001\",\"released\":true,\"denialReason\":null}")
                .addHeader("Content-Type", "application/json"));

        integrationService.validarAgendamentoParaCriacao("BK001", br.com.cloudport.servicogate.model.enums.TipoOperacao.ENTRADA);

        var recorded = mockWebServer.getRequestCount();
        assertThat(recorded).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve lançar exceção quando TOS sinalizar bloqueio do booking")
    void deveLancarExcecaoQuandoBookingNegado() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"bookingNumber\":\"BK002\",\"released\":false,\"denialReason\":\"Pendência\"}")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> integrationService.validarAgendamentoParaCriacao("BK002",
                br.com.cloudport.servicogate.model.enums.TipoOperacao.SAIDA))
                .isInstanceOf(TosIntegrationException.class)
                .hasMessageContaining("BK002")
                .hasMessageContaining("Pendência");
    }
}

