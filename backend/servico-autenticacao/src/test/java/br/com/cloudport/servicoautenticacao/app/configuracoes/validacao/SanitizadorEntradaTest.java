package br.com.cloudport.servicoautenticacao.app.configuracoes.validacao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SanitizadorEntradaTest {

    @Test
    void deveAplicarPoliticaSomenteNaDefinicaoDeNovaSenha() {
        assertDoesNotThrow(() -> SanitizadorEntrada.validarNovaSenha("Ⅳ<senha>"));
    }

    @Test
    void deveRejeitarNovaSenhaForaDoTamanhoPermitido() {
        assertThrows(IllegalArgumentException.class,
                () -> SanitizadorEntrada.validarNovaSenha("12345"));
        assertThrows(IllegalArgumentException.class,
                () -> SanitizadorEntrada.validarNovaSenha("a".repeat(256)));
    }

    @Test
    void deveRejeitarControleNaoImprimivelEmNovaSenha() {
        assertThrows(IllegalArgumentException.class,
                () -> SanitizadorEntrada.validarNovaSenha("senha\u0000"));
    }
}
