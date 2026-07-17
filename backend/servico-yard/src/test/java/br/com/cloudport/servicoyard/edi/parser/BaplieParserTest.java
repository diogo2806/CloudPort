package br.com.cloudport.servicoyard.edi.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.EstadoCargaContainer;
import br.com.cloudport.servicoyard.edi.modelo.TipoOperacaoBayPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaplieParser - atributos operacionais e de segurança")
class BaplieParserTest {

    private final BaplieParser parser = new BaplieParser();

    @Test
    @DisplayName("Deve interpretar D.95B, inferir operação e normalizar unidades de peso")
    void deveInterpretarD95bComPesoNormalizado() {
        String mensagem = "UNH+1+BAPLIE:D:95B:UN:SMDG20'"
                + "BGM+599+PLANO-1+9'"
                + "TDT+20+VOY001++1:MSC GULSEUM'"
                + "LOC+5+BRSSZ'"
                + "LOC+61+DEHAM'"
                + "EQD+CN+MSCU1234567+22G1:6:5+4+1+5'"
                + "LOC+147+010284'"
                + "LOC+9+BRSSZ'"
                + "LOC+11+DEHAM'"
                + "MEA+WT++24.5:TNE'"
                + "RFF+BM:BL001'"
                + "EQD+CN+MSCU7654321+22G1:6:5+4+1+4'"
                + "LOC+147+020286'"
                + "LOC+9+USNYC'"
                + "LOC+11+DEHAM'"
                + "MEA+WT++22046.226:LBR'";

        BayPlan bayPlan = parser.parse(mensagem);

        assertEquals("MSC GULSEUM", bayPlan.getCodigoNavio());
        assertEquals("MSC GULSEUM", bayPlan.getNomeNavio());
        assertEquals("VOY001", bayPlan.getCodigoViagem());
        assertEquals(2, bayPlan.getContainers().size());

        BayPlanContainer primeiro = bayPlan.getContainers().get(0);
        assertEquals(24_500.0, primeiro.getPesoKg(), 0.001);
        assertEquals("TNE", primeiro.getUnidadePesoOriginal());
        assertEquals(EstadoCargaContainer.CHEIO, primeiro.getEstadoCarga());
        assertEquals(TipoOperacaoBayPlan.CARREGAMENTO, primeiro.getTipoOperacao());
        assertEquals("BL001", primeiro.getReferenciaBl());
        assertEquals(1, primeiro.getPosicaoBay().getBay());
        assertEquals(2, primeiro.getPosicaoBay().getRow());
        assertEquals(84, primeiro.getPosicaoBay().getTier());

        BayPlanContainer segundo = bayPlan.getContainers().get(1);
        assertEquals(10_000.0, segundo.getPesoKg(), 0.01);
        assertEquals("LBR", segundo.getUnidadePesoOriginal());
        assertEquals(EstadoCargaContainer.VAZIO, segundo.getEstadoCarga());
        assertEquals(TipoOperacaoBayPlan.DESCARGA, segundo.getTipoOperacao());
        assertFalse(segundo.getSegmentosOriginais().isBlank());
    }

    @Test
    @DisplayName("Deve interpretar D.13B com VGM, reefer, IMO, segregação e OOG")
    void deveInterpretarD13bEstruturado() {
        String mensagem = "UNH+M-EX11+BAPLIE:D:13B:UN:SMDG31'"
                + "BGM+659::LOADONLY+M-EX1/1++38'"
                + "TDT+20+014E47+++HLC:LINES:306+++9501332::11:NEW YORK EXPRESS'"
                + "LOC+5+BEANR'"
                + "LOC+61+DEHAM'"
                + "LOC+147+0501490::5'"
                + "EQD+CN+MARU1234567+45R1:2++2++5'"
                + "MEA+AAE+WT+TNE:12'"
                + "MEA+AAE+VGM+KGM:12500'"
                + "TMP+2+-1.5:C'"
                + "RNG+5+CEL:-24:-21'"
                + "DGS+IMD+2.1+1954+055:C+II+F-E,S-E'"
                + "ATT+26+SEG:DGATT:306+SEGREGATION-A'"
                + "DIM+9+CMT:35'"
                + "HAN+RF:REEFER'";

        BayPlan bayPlan = parser.parse(mensagem);
        BayPlanContainer container = bayPlan.getContainers().get(0);

        assertEquals("9501332", bayPlan.getCodigoNavio());
        assertEquals("NEW YORK EXPRESS", bayPlan.getNomeNavio());
        assertEquals(50, container.getPosicaoBay().getBay());
        assertEquals(14, container.getPosicaoBay().getRow());
        assertEquals(90, container.getPosicaoBay().getTier());
        assertEquals(TipoOperacaoBayPlan.CARREGAMENTO, container.getTipoOperacao());
        assertEquals(12_000.0, container.getPesoKg(), 0.001);
        assertEquals(12_500.0, container.getPesoVgmKg(), 0.001);
        assertEquals(12_500.0, container.getPesoOperacionalKg(), 0.001);
        assertEquals("BAPLIE", container.getOrigemVgm());
        assertEquals("VERIFICADO", container.getStatusVgm());
        assertTrue(container.isReefer());
        assertEquals(-1.5, container.getTemperaturaRequeridaC(), 0.001);
        assertEquals(-24.0, container.getTemperaturaMinimaC(), 0.001);
        assertEquals(-21.0, container.getTemperaturaMaximaC(), 0.001);
        assertTrue(container.isPerigoso());
        assertEquals("2.1", container.getClasseImo());
        assertEquals("1954", container.getNumeroOnu());
        assertEquals("II", container.getGrupoEmbalagem());
        assertTrue(container.getGrupoSegregacao().contains("SEGREGATION-A"));
        assertTrue(container.isOog());
        assertEquals(35.0, container.getExcessoAlturaCm(), 0.001);
        assertTrue(container.getSegmentosOriginais().contains("DGS+IMD+2.1+1954"));
    }

    @Test
    @DisplayName("Deve rejeitar versão BAPLIE fora dos perfis aceitos")
    void deveRejeitarPerfilNaoSuportado() {
        String mensagem = "UNH+1+BAPLIE:D:96A:UN:SMDG20'";

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(mensagem));

        assertTrue(erro.getMessage().contains("não suportado"));
    }

    @Test
    @DisplayName("Deve rejeitar navio sem identidade real em vez de criar código sintético")
    void deveRejeitarNavioSemIdentidade() {
        String mensagem = "UNH+1+BAPLIE:D:95B:UN:SMDG20'"
                + "TDT+20+VOY001'"
                + "EQD+CN+MSCU1234567+22G1+++5'"
                + "LOC+147+010284'"
                + "MEA+WT++10000:KGM'";

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(mensagem));

        assertTrue(erro.getMessage().contains("identificação real do navio"));
    }

    @Test
    @DisplayName("Deve rejeitar equipamento sem indicador cheio ou vazio")
    void deveRejeitarEstadoCargaAusente() {
        String mensagem = "UNH+1+BAPLIE:D:95B:UN:SMDG20'"
                + "TDT+20+VOY001++1:MSC GULSEUM'"
                + "EQD+CN+MSCU1234567+22G1'"
                + "LOC+147+010284'"
                + "MEA+WT++10000:KGM'";

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(mensagem));

        assertTrue(erro.getMessage().contains("cheio/vazio"));
    }
}
