package br.com.cloudport.servicoautenticacao.app.configuracoes.validacao;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class SanitizadorEntrada {
    private static final Pattern PADRAO_LOGIN_PERMITIDO = Pattern.compile("^[\\p{L}\\p{N}@._-]{3,100}$");

    private SanitizadorEntrada() {
    }

    public static String sanitizarLogin(String login) {
        if (login == null) {
            throw new IllegalArgumentException("Login informado é inválido.");
        }
        String normalizado = Normalizer.normalize(login, Normalizer.Form.NFKC).trim();
        if (!PADRAO_LOGIN_PERMITIDO.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("Login informado contém caracteres inválidos.");
        }
        return normalizado;
    }

    public static String sanitizarSenha(String senha) {
        if (senha == null) {
            throw new IllegalArgumentException("Senha informada é inválida.");
        }
        String normalizada = Normalizer.normalize(senha, Normalizer.Form.NFKC);
        if (normalizada.contains("<") || normalizada.contains(">")) {
            throw new IllegalArgumentException("Senha não pode conter os caracteres < ou >.");
        }
        boolean possuiControle = normalizada.chars()
                .anyMatch(caractere -> Character.isISOControl(caractere) && !Character.isWhitespace(caractere));
        if (possuiControle) {
            throw new IllegalArgumentException("Senha contém caracteres de controle não permitidos.");
        }
        return normalizada;
    }
}
