package br.com.cloudport.serviconaviosiderurgico.configuracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CredenciaisSegurancaValidatorTest {

    private static final String SEGREDO_VALIDO = "01234567890123456789012345678901";
    private static final String SEGREDO_PUBLICO_VALIDO = "segredo-publico-com-32-caracteres";

    @Test
    void deveRejeitarSegredoJwtAusente() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.validarSegredoJwt(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configurado externamente");
    }

    @Test
    void deveRejeitarSegredoJwtMenorQueTrintaEDoisBytes() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.validarSegredoJwt("segredo-curto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void deveRejeitarSegredoJwtSentinelaConhecido() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.validarSegredoJwt(
                "chave-local-para-desenvolvimento-123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinela");
    }

    @Test
    void deveAceitarSegredoJwtExternoComAoMenosTrintaEDoisBytes() {
        assertThat(CredenciaisSegurancaValidator.validarSegredoJwt(SEGREDO_VALIDO))
                .isEqualTo(SEGREDO_VALIDO);
    }

    @Test
    void deveDesabilitarClientesPublicosQuandoConfiguracaoEstiverAusente() {
        assertThat(CredenciaisSegurancaValidator.carregarClientesPublicos(" "))
                .isEmpty();
    }

    @Test
    void deveRejeitarClientePublicoSentinelaConhecido() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.carregarClientesPublicos(
                "cloudport-local:" + SEGREDO_PUBLICO_VALIDO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinela");
    }

    @Test
    void deveRejeitarSegredoPublicoSentinelaConhecido() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.carregarClientesPublicos(
                "secretaria-portos:troque-esta-chave-publica"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sentinela");
    }

    @Test
    void deveRejeitarSegredoPublicoMenorQueTrintaEDoisBytes() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.carregarClientesPublicos(
                "secretaria-portos:segredo-curto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void deveRejeitarClientePublicoDuplicado() {
        assertThatThrownBy(() -> CredenciaisSegurancaValidator.carregarClientesPublicos(
                "secretaria-portos:" + SEGREDO_PUBLICO_VALIDO
                        + ",secretaria-portos:outro-segredo-publico-com-32-bytes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicado");
    }

    @Test
    void deveCarregarSomenteClientesPublicosValidos() {
        Map<String, String> clientes = CredenciaisSegurancaValidator.carregarClientesPublicos(
                "secretaria-portos:" + SEGREDO_PUBLICO_VALIDO
                        + ",operador-externo:outro-segredo-publico-com-32-bytes"
        );

        assertThat(clientes)
                .containsEntry("secretaria-portos", SEGREDO_PUBLICO_VALIDO)
                .containsEntry("operador-externo", "outro-segredo-publico-com-32-bytes")
                .hasSize(2);
    }
}
