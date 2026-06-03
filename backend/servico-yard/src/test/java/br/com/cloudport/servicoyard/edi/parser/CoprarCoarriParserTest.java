package br.com.cloudport.servicoyard.edi.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import br.com.cloudport.servicoyard.edi.parser.CoprarCoarriParser.ResultadoParse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CoprarCoarriParser - Parser COPRAR e COARRI")
class CoprarCoarriParserTest {

    private CoprarCoarriParser parser;

    private static final String COPRAR_VALIDO =
            "UNB+UNOA:2+SENDER+RECEIVER+260101:0000+1'" +
            "UNH+1+COPRAR:D:95B:UN:SMDG20'" +
            "BGM+58B+COPRAR001+9'" +
            "TDT+20+VOY001++1:MSC GULSEUM'" +
            "EQD+CN+TCKU7453652+22G1:6:5+4+1+5+1'" +
            "LOC+147+110102:3:22'" +
            "LOC+9+DEHAM:139:6'" +
            "MEA+WT++24500:KGM'" +
            "EQD+CN+MSCU9876543+45G1:6:5+4+1+5+2'" +
            "LOC+147+110202:3:22'" +
            "LOC+9+USNYC:139:6'" +
            "UNT+12+1'" +
            "UNZ+1+1'";

    private static final String COARRI_VALIDO =
            "UNB+UNOA:2+SENDER+RECEIVER+260101:0000+1'" +
            "UNH+1+COARRI:D:95B:UN:SMDG20'" +
            "BGM+58D+COARRI001+9'" +
            "TDT+20+VOY001++1:MSC GULSEUM'" +
            "EQD+CN+TCKU7453652+22G1:6:5'" +
            "DTM+334:202606031430:203'" +
            "LOC+147+110102:3:22'" +
            "FTX+AAI+++Container loaded successfully'" +
            "UNT+8+1'" +
            "UNZ+1+1'";

    @BeforeEach
    void setup() {
        parser = new CoprarCoarriParser();
    }

    @Test
    @DisplayName("COPRAR: deve extrair navio e viagem do TDT")
    void coprarExtirNavioViagem() {
        ResultadoParse r = parser.parse(COPRAR_VALIDO);

        assertEquals("VOY001", r.getCodigoViagem());
        assertNotNull(r.getCodigoNavio());
    }

    @Test
    @DisplayName("COPRAR: deve extrair 2 containers")
    void coprarExtirContainers() {
        ResultadoParse r = parser.parse(COPRAR_VALIDO);

        assertEquals(2, r.getContainers().size());
        assertEquals("TCKU7453652", r.getContainers().get(0).getCodigoContainer());
        assertEquals("MSCU9876543", r.getContainers().get(1).getCodigoContainer());
    }

    @Test
    @DisplayName("COPRAR: deve identificar operação CARREGAMENTO (função=1)")
    void coprarCarregamento() {
        ResultadoParse r = parser.parse(COPRAR_VALIDO);

        assertEquals(TipoOperacaoBayPlan.CARREGAMENTO,
                r.getContainers().get(0).getTipoOperacao());
    }

    @Test
    @DisplayName("COPRAR: deve identificar operação DESCARGA (função=2)")
    void coprarDescarga() {
        ResultadoParse r = parser.parse(COPRAR_VALIDO);

        assertEquals(TipoOperacaoBayPlan.DESCARGA,
                r.getContainers().get(1).getTipoOperacao());
    }

    @Test
    @DisplayName("COPRAR: deve extrair posição bay/row/tier")
    void coprarPosicaoBay() {
        ResultadoParse r = parser.parse(COPRAR_VALIDO);

        var pos = r.getContainers().get(0).getPosicaoBay();
        assertNotNull(pos);
        assertEquals(11, pos.getBay());
        assertEquals(1, pos.getRow());
        assertEquals(2, pos.getTier());
    }

    @Test
    @DisplayName("COARRI: deve extrair horário real da operação (DTM+334)")
    void coarriHorarioOperacao() {
        ResultadoParse r = parser.parse(COARRI_VALIDO);

        assertFalse(r.getContainers().isEmpty());
        var c = r.getContainers().get(0);
        assertNotNull(c.getHorarioOperacao());
        assertEquals(2026, c.getHorarioOperacao().getYear());
        assertEquals(6, c.getHorarioOperacao().getMonthValue());
        assertEquals(3, c.getHorarioOperacao().getDayOfMonth());
    }

    @Test
    @DisplayName("COARRI: deve marcar container como CONCLUIDO")
    void coarriStatusConcluido() {
        ResultadoParse r = parser.parse(COARRI_VALIDO);

        assertEquals("CONCLUIDO", r.getContainers().get(0).getStatusOperacao());
    }

    @Test
    @DisplayName("Deve lançar exceção para conteúdo vazio")
    void erroParaVazio() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
    }
}
