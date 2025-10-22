package br.com.cloudport.servicoautenticacao.app.seguranca;

import org.springframework.stereotype.Component;

@Component
public class SanitizadorConteudoSeguranca {

    public String sanitizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semScripts = valor.replaceAll("(?i)<script.*?>.*?</script>", "");
        String semTags = semScripts.replaceAll("<[^>]+>", "");
        return semTags
                .replace("\"", "")
                .replace("'", "")
                .replace("`", "")
                .trim();
    }
}
