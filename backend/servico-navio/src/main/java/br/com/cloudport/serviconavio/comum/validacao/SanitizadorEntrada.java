package br.com.cloudport.serviconavio.comum.validacao;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SanitizadorEntrada {
    public String limparTexto(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String limpo = Jsoup.clean(valor, Safelist.none());
        return StringUtils.hasText(limpo) ? limpo.trim() : null;
    }

    public String limparTextoObrigatorio(String valor, String descricaoCampo) {
        String resultado = limparTexto(valor);
        if (!StringUtils.hasText(resultado)) {
            throw new IllegalArgumentException("O campo " + descricaoCampo + " possui caracteres inválidos ou está vazio.");
        }
        return resultado;
    }
}
