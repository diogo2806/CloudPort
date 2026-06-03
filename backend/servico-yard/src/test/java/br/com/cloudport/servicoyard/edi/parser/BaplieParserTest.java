package br.com.cloudport.servicoyard.edi.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaplieParser - Parser EDIFACT D.95B")
class BaplieParserTest {

    private BaplieParser parser;

    // BAPLIE minimal válido com 2 containers
    private static final String BAPLIE_VALIDO =
            "UNB+UNOA:2+SENDER+RECEIVER+260101:0000+1'" +
            "UNH+1+BAPLIE:D:95B:UN:SMDG20'" +
            "BGM+599+BAYPLAN001+9'" +
            "TDT+20+VOY001++1:MSC GULSEUM'" +
            "LOC+5+BRSSZ:139:6'" +
            "LOC+61+DEHAM:139:6'" +
            "EQD+CN+TCKU7453652+22G1:6:5+4+1+5'" +
            "LOC+147+110102:3:22'" +
            "LOC+9+DEHAM:139:6'" +
            "LOC+11+BRSSZ:139:6'" +
            "MEA+WT++24500:KGM'" +
            "RFF+BL:MSCUBL123456'" +
            "EQD+CN+MSCU9876543+45G1:6:5+4+1+5'" +
            "LOC+147+110202:3:22'" +
            "LOC+9+USNYC:139:6'" +
            "MEA+WT++18200:KGM'" +
            "UNT+16+1'" +
            "UNZ+1+1'";

    @BeforeEach
    void setup() {
        parser = new BaplieParser();
    }

    @Test
    @DisplayName("Deve extrair dados do navio (TDT)")
    void extrairDadosNavio() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertNotNull(bp.getCodigoNavio());
        assertEquals("VOY001", bp.getCodigoViagem());
        assertTrue(bp.getNomeNavio().contains("MSC GULSEUM"));
    }

    @Test
    @DisplayName("Deve extrair portos de carga e descarga")
    void extrairPortos() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals("BRSSZ", bp.getPortoCarga());
        assertEquals("DEHAM", bp.getPortoDescarga());
    }

    @Test
    @DisplayName("Deve extrair 2 containers com códigos corretos")
    void extrairContainers() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals(2, bp.getContainers().size());
        assertEquals("TCKU7453652", bp.getContainers().get(0).getCodigoContainer());
        assertEquals("MSCU9876543", bp.getContainers().get(1).getCodigoContainer());
    }

    @Test
    @DisplayName("Deve extrair posição bay/row/tier do container")
    void extrairPosicaoBay() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        var pos = bp.getContainers().get(0).getPosicaoBay();
        assertNotNull(pos);
        assertEquals(11, pos.getBay());
        assertEquals(1, pos.getRow());
        assertEquals(2, pos.getTier());
    }

    @Test
    @DisplayName("Deve extrair porto de descarga do container")
    void extrairPortoDescargaContainer() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals("DEHAM", bp.getContainers().get(0).getPortoDescarga());
        assertEquals("USNYC", bp.getContainers().get(1).getPortoDescarga());
    }

    @Test
    @DisplayName("Deve extrair peso bruto")
    void extrairPeso() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals(24500.0, bp.getContainers().get(0).getPesoKg());
        assertEquals(18200.0, bp.getContainers().get(1).getPesoKg());
    }

    @Test
    @DisplayName("Deve extrair referência Bill of Lading")
    void extrairReferenciaBl() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals("MSCUBL123456", bp.getContainers().get(0).getReferenciaBl());
    }

    @Test
    @DisplayName("Deve definir status RASCUNHO e origem BAPLIE")
    void statusEOrigem() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertEquals(StatusBayPlan.RASCUNHO, bp.getStatus());
        assertEquals("BAPLIE", bp.getOrigemMensagem());
    }

    @Test
    @DisplayName("Deve definir tipo operação DESCARGA para containers BAPLIE")
    void tipoOperacaoDescarga() {
        BayPlan bp = parser.parse(BAPLIE_VALIDO);

        assertTrue(bp.getContainers().stream()
                .allMatch(c -> c.getTipoOperacao() == TipoOperacaoBayPlan.DESCARGA));
    }

    @Test
    @DisplayName("Deve lançar exceção para EDIFACT vazio")
    void erroParaEdifactVazio() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("Deve lançar exceção se TDT ausente")
    void erroSemTdt() {
        String semTdt = "UNB+UNOA:2+S+R+260101:0000+1'" +
                        "EQD+CN+CONT001+22G1'" +
                        "LOC+147+110102:3:22'";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(semTdt));
    }
}
