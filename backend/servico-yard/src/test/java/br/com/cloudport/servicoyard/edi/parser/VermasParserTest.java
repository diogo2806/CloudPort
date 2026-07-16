package br.com.cloudport.servicoyard.edi.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VermasParserTest {

    private final VermasParser parser = new VermasParser();

    @Test
    void deveExtrairContainerEPesoVgmEmQuilogramas() {
        String mensagem = "UNH+1+VERMAS:D:16A:UN'"
                + "TDT+20+V001+1++MSC:172:20+++IMO1234567:146::NAVIO TESTE'"
                + "EQD+CN+MSCU1234567+22G1:102:5'"
                + "MEA+AAE+VGM+KGM:24500'"
                + "UNT+4+1'";

        VermasParser.ResultadoVermas resultado = parser.parse(mensagem);

        assertEquals("V001", resultado.codigoViagem());
        assertEquals(1, resultado.pesos().size());
        assertEquals("MSCU1234567", resultado.pesos().get(0).codigoContainer());
        assertEquals(24500D, resultado.pesos().get(0).pesoKg());
    }

    @Test
    void deveConverterToneladasParaQuilogramas() {
        String mensagem = "UNH+2+VERMAS:D:16A:UN'"
                + "EQD+CN+MSCU7654321+22G1:102:5'"
                + "MEA+AAE+VGM+TNE:24.5'"
                + "UNT+3+2'";

        VermasParser.ResultadoVermas resultado = parser.parse(mensagem);

        assertEquals(24500D, resultado.pesos().get(0).pesoKg());
    }

    @Test
    void deveRejeitarMensagemSemVgm() {
        String mensagem = "UNH+3+VERMAS:D:16A:UN'EQD+CN+MSCU1234567+22G1:102:5'UNT+2+3'";

        assertThrows(IllegalArgumentException.class, () -> parser.parse(mensagem));
    }
}
