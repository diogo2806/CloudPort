package br.com.cloudport.servicoyard.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import javax.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ObservabilidadeYardFiltroTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void devePreservarTraceERegistrarContextoDaWorkQueue() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservabilidadeYardFiltro filtro = new ObservabilidadeYardFiltro(
                meterRegistry,
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/yard/patio/work-queues/9/dispatch"
        );
        request.addHeader(ObservabilidadeYardFiltro.HEADER_CORRELATION_ID, "corr-yard-009");
        request.addHeader(
                ObservabilidadeYardFiltro.HEADER_TRACEPARENT,
                "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"
        );
        request.addHeader("X-CloudPort-Equipment-Id", "RTG-01");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (requisicao, resposta) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("corr-yard-009");
            assertThat(MDC.get("traceId")).isEqualTo("0123456789abcdef0123456789abcdef");
            assertThat(MDC.get("workQueueId")).isEqualTo("9");
            assertThat(MDC.get("equipamentoId")).isEqualTo("RTG-01");
        };

        filtro.doFilter(request, response, chain);

        assertThat(response.getHeader(ObservabilidadeYardFiltro.HEADER_CORRELATION_ID))
                .isEqualTo("corr-yard-009");
        assertThat(response.getHeader(ObservabilidadeYardFiltro.HEADER_TRACE_ID))
                .isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(meterRegistry.counter(
                "cloudport.operacao.total",
                "modulo", "yard",
                "operacao", "POST /yard/patio/work-queues/{id}/dispatch",
                "resultado", "sucesso",
                "contexto", "equipamento"
        ).count()).isEqualTo(1.0d);
        assertThat(MDC.get("correlationId")).isNull();
    }
}
