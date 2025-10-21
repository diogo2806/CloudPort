package br.com.cloudport.servicorail.comum.validacao;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ValidacaoEntradaUtil {

    private static final Pattern PADRAO_TEXTO_SEGURO = Pattern.compile("^[\\p{L}0-9\\s\\-_/.:]*$");

    private ValidacaoEntradaUtil() {
    }

    public static String limparTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String ajustado = valor.trim();
        validarConteudo(ajustado);
        return ajustado;
    }

    public static List<String> limparLista(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        return valores.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .peek(ValidacaoEntradaUtil::validarConteudo)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    private static void validarConteudo(String valor) {
        if (valor.isEmpty()) {
            return;
        }
        if (valor.contains("<") || valor.contains(">")) {
            throw new IllegalArgumentException("Valor contém caracteres não permitidos.");
        }
        if (!PADRAO_TEXTO_SEGURO.matcher(valor).matches()) {
            throw new IllegalArgumentException("Valor contém caracteres inválidos.");
        }
    }
}
