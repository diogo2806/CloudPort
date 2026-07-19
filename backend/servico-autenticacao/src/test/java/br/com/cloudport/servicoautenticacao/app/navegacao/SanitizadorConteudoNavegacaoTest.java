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
    void deveDecodificarEntidadesHtmlSimplesEDuplamenteCodificadas() {
        assertEquals(
                "Políticas de Segurança",
                sanitizador.sanitizarRotulo("Pol&amp;iacute;ticas de Seguran&amp;ccedil;a")
        );
        assertEquals(
                "Lost & Found / TBD",
                sanitizador.sanitizarRotulo("Lost &amp; Found / TBD")
        );
    }

    @Test
    void deveRemoverCaracteresDeControleSemCodificarComoHtml() {
        String comControle = "  Gestão" + Character.toString(0) + " & Recursos\n  ";

        assertEquals("Gestão & Recursos", sanitizador.sanitizarMensagem(comControle));
    }

    @Test
    void deveRemoverMarcacaoHtmlDosTextos() {
        assertEquals(
                "alert('x')Pátio",
                sanitizador.sanitizarRotulo("&lt;script&gt;alert('x')&lt;/script&gt;P&amp;aacute;tio")
        );
    }

    @Test
    void deveExibirNomesLegiveisParaOsGruposDoMenu() {
        assertEquals("Visão geral", sanitizador.sanitizarGrupo("VISAO_GERAL"));
        assertEquals("Configurações", sanitizador.sanitizarGrupo("CONFIGURACOES"));
        assertEquals("Navio e embarque", sanitizador.sanitizarGrupo("Navio e embarque"));
        assertEquals("Pátio", sanitizador.sanitizarGrupo("PÁTIO"));
    }

    @Test
    void deveContinuarNormalizandoCamposTecnicos() {
        assertEquals("patio/gestao-de-recursos", sanitizador.sanitizarIdentificador("Pátio/Gestão de Recursos"));
        assertEquals(Arrays.asList("patio", "gestao-de-recursos"),
                sanitizador.sanitizarSegmentosRota("Pátio/Gestão de Recursos"));
        assertEquals(Arrays.asList("ROLE_ADMIN_PORTO", "ROLE_OPERADOR_PATIO"),
                sanitizador.sanitizarListaPapeis("ROLE_ADMIN_PORTO, ROLE_OPERADOR_PÁTIO"));
    }
}
