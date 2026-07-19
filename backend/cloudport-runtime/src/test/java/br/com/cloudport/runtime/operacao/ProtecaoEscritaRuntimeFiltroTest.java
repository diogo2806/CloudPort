package br.com.cloudport.runtime.operacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProtecaoEscritaRuntimeFiltroTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveBloquearComandoQuandoRuntimeEstaEmObservacao() throws Exception {
        ProtecaoEscritaRuntimeFiltro filtro = new ProtecaoEscritaRuntimeFiltro(false, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/navios");
        request.addHeader("X-Correlation-Id", "corte-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean executouCadeia = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> executouCadeia.set(true));

        assertThat(executouCadeia).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("corte-123");
        JsonNode erro = objectMapper.readTree(response.getContentAsString());
        assertThat(erro.path("codigo").asText()).isEqualTo("RUNTIME_SOMENTE_LEITURA");
        assertThat(erro.path("caminho").asText()).isEqualTo("/navios");
        assertThat(erro.path("metodo").asText()).isEqualTo("POST");
    }

    @Test
    void devePermitirLeituraMesmoQuandoEscritaEstaDesabilitada() throws Exception {
        ProtecaoEscritaRuntimeFiltro filtro = new ProtecaoEscritaRuntimeFiltro(false, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/navios/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean executouCadeia = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> executouCadeia.set(true));

        assertThat(executouCadeia).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void devePermitirLoginMesmoQuandoRuntimeEstaEmObservacao() throws Exception {
        ProtecaoEscritaRuntimeFiltro filtro = new ProtecaoEscritaRuntimeFiltro(false, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean executouCadeia = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> executouCadeia.set(true));

        assertThat(executouCadeia).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void devePermitirComandoSomenteNaInstanciaEscritora() throws Exception {
        ProtecaoEscritaRuntimeFiltro filtro = new ProtecaoEscritaRuntimeFiltro(true, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/yard/patio/ordens/10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean executouCadeia = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> executouCadeia.set(true));

        assertThat(executouCadeia).isTrue();
    }
}
