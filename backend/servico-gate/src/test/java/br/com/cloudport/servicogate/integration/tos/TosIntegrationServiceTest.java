package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import br.com.cloudport.servicogate.app.administracao.ContingenciaProperties;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Collectors;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.CacheManager;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(OutputCaptureExtension.class)
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
        properties.getRetry().setMaxAttempts(1);
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
        MDC.clear();
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Deve validar criação de agendamento quando booking está liberado no TOS")
    void deveValidarAgendamentoQuandoBookingLiberado() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"bookingNumber\":\"BK001\",\"released\":true,\"denialReason\":null}")
                .addHeader("Content-Type", "application/json"));

        integrationService.validarAgendamentoParaCriacao("BK001", br.com.cloudport.servicogate.model.enums.TipoOperacao.ENTRADA);

        int recorded = mockWebServer.getRequestCount();
        assertThat(recorded).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve omitir motivo sensível quando TOS sinalizar bloqueio do booking")
    void deveOmitirMotivoSensivelQuandoBookingNegado() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"bookingNumber\":\"BK002\",\"released\":false,\"denialReason\":\"Pendência aduaneira sigilosa\"}")
                .addHeader("Content-Type", "application/json"));

        Throwable falha = catchThrowable(() -> integrationService.validarAgendamentoParaCriacao(
                "BK002",
                br.com.cloudport.servicogate.model.enums.TipoOperacao.SAIDA));

        assertThat(falha)
                .isInstanceOf(TosIntegrationException.class)
                .hasMessageContaining("BK***02")
                .hasMessageNotContaining("BK002")
                .hasMessageNotContaining("Pendência aduaneira sigilosa");
    }

    @Test
    @DisplayName("Deve registrar somente metadados permitidos para resposta HTTP de erro do TOS")
    void deveSanitizarRespostaHttpDoTos(CapturedOutput output) {
        MDC.put("correlationId", "corr-sec70-001");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setBody("{\"code\":\"BOOKING_BLOCKED\",\"token\":\"token-super-secreto\",\"document\":\"12345678900\",\"message\":\"detalhe aduaneiro reservado\"}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "session=credencial-secreta"));

        Throwable falha = catchThrowable(() -> integrationService.obterBookingInfo("BK123456"));

        assertThat(falha)
                .isInstanceOf(TosIntegrationException.class)
                .hasMessageContaining("status 409")
                .hasMessageContaining("BOOKING_BLOCKED")
                .hasMessageContaining("BK***56")
                .hasMessageNotContaining("BK123456")
                .hasMessageNotContaining("token-super-secreto")
                .hasMessageNotContaining("12345678900")
                .hasMessageNotContaining("detalhe aduaneiro reservado");

        String logsTos = output.getAll().lines()
                .filter(linha -> linha.contains("event=tos."))
                .collect(Collectors.joining("\n"));
        assertThat(logsTos)
                .contains("event=tos.call.error")
                .contains("resource=booking")
                .contains("identifier=BK***56")
                .contains("status=409")
                .contains("errorCode=BOOKING_BLOCKED")
                .contains("correlationId=corr-sec70-001")
                .doesNotContain("BK123456")
                .doesNotContain("token-super-secreto")
                .doesNotContain("12345678900")
                .doesNotContain("detalhe aduaneiro reservado")
                .doesNotContain("credencial-secreta");
    }
}
