package br.com.cloudport.serviconaviosiderurgico.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ModoEscritaSiderurgicoFiltroTest {

    private final ModoEscritaSiderurgicoFiltro filtro = new ModoEscritaSiderurgicoFiltro();

    @Test
    void deveBloquearComandoDeEscritaNoDeploymentLegado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/navios-siderurgicos");
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/navios-siderurgicos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean cadeiaExecutada = new AtomicBoolean(false);

        filtro.doFilter(request, response, (requisicao, resposta) -> cadeiaExecutada.set(true));

        assertTrue(cadeiaExecutada.get());
    }
}
