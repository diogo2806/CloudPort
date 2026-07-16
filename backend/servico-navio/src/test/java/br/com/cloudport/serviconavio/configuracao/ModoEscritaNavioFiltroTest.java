package br.com.cloudport.serviconavio.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ModoEscritaNavioFiltroTest {

    private final ModoEscritaNavioFiltro filtro = new ModoEscritaNavioFiltro();

    @Test
    void deveBloquearComandoDeEscritaNoDeploymentLegado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/navios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean cadeiaExecutada = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> cadeiaExecutada.set(true));

        assertEquals(503, response.getStatus());
        assertEquals("30", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("RUNTIME_LEGADO_SOMENTE_LEITURA"));
        assertFalse(cadeiaExecutada.get());
    }

    @Test
    void devePermitirOperacaoDeLeituraNoDeploymentLegado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/navios");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean cadeiaExecutada = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> cadeiaExecutada.set(true));

        assertTrue(cadeiaExecutada.get());
    }
}
