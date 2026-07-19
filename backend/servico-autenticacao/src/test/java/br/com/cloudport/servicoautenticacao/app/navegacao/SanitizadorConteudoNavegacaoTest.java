package br.com.cloudport.servicoautenticacao.app.navegacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SanitizadorConteudoNavegacaoTest {

    private final SanitizadorConteudoNavegacao sanitizador = new SanitizadorConteudoNavegacao();

    @Test
    void devePreservarAcentosESimbolosDosRotulos() {
        assertEquals("Line-up Ferroviário", sanitizador.sanitizarRotulo("Line-up Ferroviário"));
        assertEquals("Inventário de Contêineres", sanitizador.sanitizarRotulo("Inventário de Contêineres"));
        assertEquals("Lost & Found / TBD", sanitizador.sanitizarRotulo("Lost & Found / TBD"));
        assertEquals("Gestão de Recursos", sanitizador.sanitizarRotulo("Gestão de Recursos"));
    }

    @Test
    void deveRemoverCaracteresDeControleSemCodificarComoHtml() {
        assertEquals("Gestão & Recursos", sanitizador.sanitizarMensagem("  Gestão\u0000 & Recursos\n  "));
    }

    @Test
    void deveContinuarNormalizandoCamposTecnicos() {
        assertEquals("patio/gestao-de-recursos", sanitizador.sanitizarIdentificador("Pátio/Gestão de Recursos"));
        assertEquals("PATIO", sanitizador.sanitizarGrupo("Pátio"));
        assertEquals(Arrays.asList("patio", "gestao-de-recursos"),
                sanitizador.sanitizarSegmentosRota("Pátio/Gestão de Recursos"));
        assertEquals(Arrays.asList("ROLE_ADMIN_PORTO", "ROLE_OPERADOR_PATIO"),
                sanitizador.sanitizarListaPapeis("ROLE_ADMIN_PORTO, ROLE_OPERADOR_PÁTIO"));
    }
}
