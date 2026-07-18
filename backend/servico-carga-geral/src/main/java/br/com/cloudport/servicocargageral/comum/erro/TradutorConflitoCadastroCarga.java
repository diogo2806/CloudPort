package br.com.cloudport.servicocargageral.comum.erro;

import java.util.Locale;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

public final class TradutorConflitoCadastroCarga {

    private static final Map<String, Conflito> CONFLITOS_POR_RESTRICAO = Map.of(
            "conhecimento_carga_numero_key",
            new Conflito("BILL_OF_LADING_DUPLICADO", "Já existe um Bill of Lading com esse número."),
            "uk_conhecimento_carga_numero",
            new Conflito("BILL_OF_LADING_DUPLICADO", "Já existe um Bill of Lading com esse número."),
            "uk_item_conhecimento_sequencia",
            new Conflito("ITEM_CONHECIMENTO_DUPLICADO", "A sequência do item já existe no conhecimento."),
            "lote_carga_codigo_key",
            new Conflito("CARGO_LOT_DUPLICADO", "Já existe um cargo lot com esse código."),
            "uk_lote_carga_codigo",
            new Conflito("CARGO_LOT_DUPLICADO", "Já existe um cargo lot com esse código."),
            "uk_referencia_carga_categoria_codigo",
            new Conflito("REFERENCIA_CARGA_DUPLICADA", "A referência já existe nessa categoria."));

    private TradutorConflitoCadastroCarga() {
    }

    public static RuntimeException traduzir(DataIntegrityViolationException exception) {
        String nomeRestricao = localizarNomeRestricao(exception);
        if (nomeRestricao == null) {
            return exception;
        }

        Conflito conflito = CONFLITOS_POR_RESTRICAO.get(nomeRestricao.toLowerCase(Locale.ROOT));
        if (conflito == null) {
            return exception;
        }

        return new ConflitoCadastroCargaException(conflito.codigo(), conflito.mensagem(), exception);
    }

    private static String localizarNomeRestricao(Throwable throwable) {
        Throwable atual = throwable;
        while (atual != null) {
            if (atual instanceof ConstraintViolationException constraintViolationException) {
                return constraintViolationException.getConstraintName();
            }
            atual = atual.getCause();
        }
        return null;
    }

    private record Conflito(String codigo, String mensagem) {
    }
}
