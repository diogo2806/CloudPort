package br.com.cloudport.serviconaviosiderurgico.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ObservabilidadeOperacionalFiltroTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void devePropagarTracingExtrairContextoERegistrarMetricas() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilidadeOperacionalFiltro filtro = new ObservabilidadeOperacionalFiltro(
                meterRegistry,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/visitas-navio/42/itens/7"
        );
        request.addHeader(ObservabilidadeOperacionalFiltro.HEADER_CORRELATION_ID, "corr-contrato-001");
        request.addHeader(
                ObservabilidadeOperacionalFiltro.HEADER_TRACEPARENT,
                "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (requisicao, resposta) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("corr-contrato-001");
            assertThat(MDC.get("traceId")).isEqualTo("0123456789abcdef0123456789abcdef");
            assertThat(MDC.get("visitaId")).isEqualTo("42");
            assertThat(MDC.get("itemId")).isEqualTo("7");
        };

        filtro.doFilter(request, response, chain);

        assertThat(response.getHeader(ObservabilidadeOperacionalFiltro.HEADER_CORRELATION_ID))
                .isEqualTo("corr-contrato-001");
        assertThat(response.getHeader(ObservabilidadeOperacionalFiltro.HEADER_TRACE_ID))
                .isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(response.getHeader(ObservabilidadeOperacionalFiltro.HEADER_TRACEPARENT))
                .startsWith("00-0123456789abcdef0123456789abcdef-");
        assertThat(meterRegistry.counter(
                "cloudport.operacao.total",
                "modulo", "navio-siderurgico",
                "operacao", "GET /visitas-navio/{id}/itens/{id}",
                "resultado", "sucesso",
                "contexto", "item"
        ).count()).isEqualTo(1.0d);
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }
}
