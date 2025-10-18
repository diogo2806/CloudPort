package br.com.cloudport.servicoautenticacao.app.privacidade;

import org.springframework.stereotype.Component;

@Component
public class SanitizadorConteudoPrivacidade {

    public String sanitizarDescricao(String descricao) {
        if (descricao == null) {
            return "";
        }
        String semScripts = descricao.replaceAll("(?i)<script.*?>.*?</script>", "");
        String semTags = semScripts.replaceAll("<[^>]+>", "");
        return semTags
                .replace("\"", "")
                .replace("'", "")
                .replace("`", "")
                .trim();
    }
}
