package br.com.cloudport.monolitonavio.controlador;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ConfiguracaoFrontendControladorTest {

    @Test
    void deveEntregarApiMesmaOrigemEOrigensConfiaveisSemDuplicacao() {
        ConfiguracaoFrontendControlador controlador = new ConfiguracaoFrontendControlador(
                "  ",
                "https://portal.cloudport.local, https://portal.cloudport.local, http://localhost:4200");

        ConfiguracaoFrontendControlador.ConfiguracaoFrontendResposta resposta =
                controlador.obterConfiguracao();

        assertEquals("", resposta.getBaseApiUrl());
        assertEquals(
                Arrays.asList("https://portal.cloudport.local", "http://localhost:4200"),
                resposta.getTrustedParentOrigins());
    }

    @Test
    void deveUsarOrigemLocalQuandoConfiguracaoEstiverVazia() {
        ConfiguracaoFrontendControlador controlador = new ConfiguracaoFrontendControlador(
                "https://navio.cloudport.local/api/",
                " ");

        ConfiguracaoFrontendControlador.ConfiguracaoFrontendResposta resposta =
                controlador.obterConfiguracao();

        assertEquals("https://navio.cloudport.local/api/", resposta.getBaseApiUrl());
        assertEquals(Arrays.asList("http://localhost:4200"), resposta.getTrustedParentOrigins());
    }
}
