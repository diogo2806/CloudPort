package br.com.cloudport.servicoautenticacao.app.notificacoes;

import java.text.Normalizer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class SanitizadorConteudoNotificacao {

    public String sanitizarNomeCanal(String nomeCanal) {
        if (nomeCanal == null) {
            return "";
        }
        String normalizado = Normalizer.normalize(nomeCanal, Normalizer.Form.NFKC).trim();
        String semCaracteresDeControle = normalizado.replaceAll("[\\p{Cntrl}]", "");
        return HtmlUtils.htmlEscape(semCaracteresDeControle);
    }
}
