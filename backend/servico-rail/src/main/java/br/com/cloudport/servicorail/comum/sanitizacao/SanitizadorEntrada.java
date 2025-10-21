package br.com.cloudport.servicorail.comum.sanitizacao;

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
}
