package br.com.cloudport.servicocargageral.comum.erro;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataIntegrityViolationException;

class TradutorConflitoCadastroCargaTest {

    @ParameterizedTest
    @MethodSource("restricoesConhecidas")
    void deveTraduzirSomenteRestricoesUnicasConhecidas(
            String restricao,
            String codigo,
            String mensagem) {
        DataIntegrityViolationException violacao = violacao(restricao);

        RuntimeException resultado = TradutorConflitoCadastroCarga.traduzir(violacao);

        assertThat(resultado).isInstanceOf(ConflitoCadastroCargaException.class);
        ConflitoCadastroCargaException conflito = (ConflitoCadastroCargaException) resultado;
        assertThat(conflito.getCodigo()).isEqualTo(codigo);
        assertThat(conflito.getMessage()).isEqualTo(mensagem);
        assertThat(conflito.getCause()).isSameAs(violacao);
    }

    @Test
    void devePreservarFalhaDeIntegridadeDesconhecida() {
        DataIntegrityViolationException violacao = violacao("uk_restricao_nao_mapeada");

        RuntimeException resultado = TradutorConflitoCadastroCarga.traduzir(violacao);

        assertThat(resultado).isSameAs(violacao);
    }

    private static Stream<Arguments> restricoesConhecidas() {
        return Stream.of(
                Arguments.of(
                        "conhecimento_carga_numero_key",
                        "BILL_OF_LADING_DUPLICADO",
                        "Já existe um Bill of Lading com esse número."),
                Arguments.of(
                        "uk_item_conhecimento_sequencia",
                        "ITEM_CONHECIMENTO_DUPLICADO",
                        "A sequência do item já existe no conhecimento."),
                Arguments.of(
                        "lote_carga_codigo_key",
                        "CARGO_LOT_DUPLICADO",
                        "Já existe um cargo lot com esse código."),
                Arguments.of(
                        "uk_referencia_carga_categoria_codigo",
                        "REFERENCIA_CARGA_DUPLICADA",
                        "A referência já existe nessa categoria."));
    }

    private static DataIntegrityViolationException violacao(String restricao) {
        SQLException sqlException = new SQLException("violação de unicidade", "23505");
        ConstraintViolationException hibernateException =
                new ConstraintViolationException("restrição violada", sqlException, restricao);
        return new DataIntegrityViolationException("falha ao persistir", hibernateException);
    }
}
