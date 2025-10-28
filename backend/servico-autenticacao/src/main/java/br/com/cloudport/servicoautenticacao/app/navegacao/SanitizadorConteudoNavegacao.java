package br.com.cloudport.servicoautenticacao.app.navegacao;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class SanitizadorConteudoNavegacao {

    public String sanitizarIdentificador(String identificador) {
        String textoLimpo = sanitizarTextoBasico(identificador);
        String semAcentos = Normalizer.normalize(textoLimpo, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");
        String apenasPermitidos = semAcentos.replaceAll("[^a-zA-Z0-9\\-/]", "-");
        String semRepeticoes = apenasPermitidos.replaceAll("-{2,}", "-");
        return semRepeticoes.toLowerCase(Locale.ROOT).replaceAll("(^-+|-+$)", "");
    }

    public String sanitizarRotulo(String rotulo) {
        return sanitizarTextoBasico(rotulo);
    }

    public String sanitizarMensagem(String mensagem) {
        return sanitizarTextoBasico(mensagem);
    }

    public String sanitizarGrupo(String grupo) {
        String texto = sanitizarTextoBasico(grupo);
        if (texto.isEmpty()) {
            return "OUTROS";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Z0-9]", "")
                .toUpperCase(Locale.ROOT);
    }

    public List<String> sanitizarSegmentosRota(String rota) {
        return Arrays.stream((rota == null ? "" : rota).split("/"))
                .map(this::sanitizarIdentificador)
                .filter(segmento -> !segmento.isEmpty())
                .collect(Collectors.toList());
    }

    public List<String> sanitizarListaPapeis(String papeis) {
        return Arrays.stream((papeis == null ? "" : papeis).split(","))
                .map(this::sanitizarPapel)
                .filter(papel -> !papel.isEmpty())
                .collect(Collectors.toList());
    }

    private String sanitizarPapel(String papel) {
        String texto = sanitizarTextoBasico(papel);
        if (texto.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Z0-9_]", "")
                .toUpperCase(Locale.ROOT);
    }

    private String sanitizarTextoBasico(String valor) {
        if (valor == null) {
            return "";
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFKC).trim();
        String semControle = normalizado.replaceAll("[\\p{Cntrl}]", "");
        return HtmlUtils.htmlEscape(semControle).trim();
    }
}
