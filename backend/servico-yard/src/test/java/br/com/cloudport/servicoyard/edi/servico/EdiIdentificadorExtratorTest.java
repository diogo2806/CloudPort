package br.com.cloudport.servicoyard.edi.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EdiIdentificadorExtratorTest {

    private final EdiIdentificadorExtrator extrator = new EdiIdentificadorExtrator();

    @Test
    void deveExtrairIdentificadoresUnbEUnh() {
        String mensagem = "UNB+UNOA:2+SENDER+RECEIVER+260716:1200+CTRL-001'"
                + "UNH+MSG-009+BAPLIE:D:95B:UN'UNT+2+MSG-009'UNZ+1+CTRL-001'";

        IdentificadoresEdi identificadores = extrator.extrair(mensagem);

        assertEquals("CTRL-001", identificadores.interchangeControlReference());
        assertEquals("MSG-009", identificadores.messageReferenceNumber());
    }

    @Test
    void deveRespeitarSeparadoresDeclaradosNoUna() {
        String mensagem = "UNA*;.! ~UNB;UNOA*2;SENDER;RECEIVER;260716*1200;CTRL-UNA~"
                + "UNH;MSG-UNA;BAPLIE*D*95B*UN~";

        IdentificadoresEdi identificadores = extrator.extrair(mensagem);

        assertEquals("CTRL-UNA", identificadores.interchangeControlReference());
        assertEquals("MSG-UNA", identificadores.messageReferenceNumber());
    }

    @Test
    void deveRetornarIdentificadoresNulosQuandoMensagemNaoTemEnvelope() {
        IdentificadoresEdi identificadores = extrator.extrair("LOC+147+0010204'");

        assertNull(identificadores.interchangeControlReference());
        assertNull(identificadores.messageReferenceNumber());
    }
}
