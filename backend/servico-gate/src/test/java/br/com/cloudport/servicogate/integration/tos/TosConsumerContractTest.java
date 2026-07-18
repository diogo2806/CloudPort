package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicogate.app.administracao.ContingenciaProperties;
import br.com.cloudport.servicogate.app.gestor.dto.TosBookingInfo;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TosConsumerContractTest {

    private MockWebServer mockWebServer;
    private TosIntegrationService integrationService;

    @BeforeEach
    void configurar() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        TosProperties properties = new TosProperties();
        properties.getApi().setBaseUrl(mockWebServer.url("/").toString());
        properties.getRetry().setMaxAttempts(1);

        TosClientConfig config = new TosClientConfig();
        IntegracaoDegradacaoHandler degradacaoHandler = new IntegracaoDegradacaoHandler(
                new GateMetrics(new SimpleMeterRegistry()));
        TosClient client = new TosClient(
                config.tosWebClient(properties),
                properties,
                config.tosRetry(properties),
                config.tosCircuitBreaker(CircuitBreakerRegistry.ofDefaults()),
                degradacaoHandler,
                new ContingenciaProperties());

        integrationService = new TosIntegrationService(
                client,
                new TosResponseAdapter(),
                config.cacheManager(properties));
    }

    @AfterEach
    void encerrar() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Contrato: deve mapear booking liberado vindo do TOS")
    void deveRespeitarContratoBookingLiberado() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"bookingNumber\":\"BK999\",\"released\":true,"
                        + "\"denialReason\":null,\"vessel\":\"Cloud Express\","
                        + "\"voyage\":\"CP-01\"}"));

        TosBookingInfo info = integrationService.obterBookingInfo("BK999");

        assertThat(info).isNotNull();
        assertThat(info.getBookingNumber()).isEqualTo("BK999");
        assertThat(info.isLiberado()).isTrue();
        assertThat(info.getVessel()).isEqualTo("Cloud Express");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/tos/bookings/BK999");
    }
}
