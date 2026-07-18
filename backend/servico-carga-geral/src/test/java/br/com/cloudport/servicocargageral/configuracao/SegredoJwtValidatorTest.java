package br.com.cloudport.servicocargageral.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SegredoJwtValidatorTest {

    @Test
    void rejeitaSegredoAusente() {
        assertThrows(IllegalStateException.class, () -> SegredoJwtValidator.validar(null));
        assertThrows(IllegalStateException.class, () -> SegredoJwtValidator.validar("   "));
    }

    @Test
    void rejeitaSegredoCurtoOuSentinela() {
        assertThrows(IllegalStateException.class, () -> SegredoJwtValidator.validar("segredo-curto"));
        assertThrows(
                IllegalStateException.class,
                () -> SegredoJwtValidator.validar("chave-local-para-desenvolvimento-123456")
        );
    }

    @Test
    void aceitaSegredoExternoComAoMenosTrintaEDoisBytes() {
        String segredo = " 0123456789abcdef0123456789abcdef ";

        assertEquals("0123456789abcdef0123456789abcdef", SegredoJwtValidator.validar(segredo));
    }
}
