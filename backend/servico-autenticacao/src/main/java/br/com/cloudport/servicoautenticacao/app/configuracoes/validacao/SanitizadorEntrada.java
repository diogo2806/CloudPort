package br.com.cloudport.servicoautenticacao.app.configuracoes.validacao;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class SanitizadorEntrada {
    private static final Pattern PADRAO_LOGIN_PERMITIDO = Pattern.compile("^[\\p{L}\\p{N}@._-]{3,100}$");
    private static final int TAMANHO_MINIMO_NOVA_SENHA = 6;
    private static final int TAMANHO_MAXIMO_NOVA_SENHA = 255;

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

    public static void validarNovaSenha(String senha) {
        if (senha == null
                || senha.length() < TAMANHO_MINIMO_NOVA_SENHA
                || senha.length() > TAMANHO_MAXIMO_NOVA_SENHA) {
            throw new IllegalArgumentException("A nova senha deve ter entre 6 e 255 caracteres.");
        }

        boolean possuiControle = senha.chars()
                .anyMatch(caractere -> Character.isISOControl(caractere) && !Character.isWhitespace(caractere));
        if (possuiControle) {
            throw new IllegalArgumentException("A nova senha contém caracteres de controle não permitidos.");
        }
    }
}
