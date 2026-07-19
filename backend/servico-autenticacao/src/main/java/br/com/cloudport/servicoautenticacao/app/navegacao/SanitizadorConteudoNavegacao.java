package br.com.cloudport.servicoautenticacao.app.navegacao;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class SanitizadorConteudoNavegacao {

    private static final Map<String, String> ROTULOS_GRUPO = Map.ofEntries(
            Map.entry("VISAOGERAL", "Visão geral"),
            Map.entry("CONFIGURACOES", "Configurações"),
            Map.entry("USUARIOS", "Usuários"),
            Map.entry("GATE", "Gate"),
            Map.entry("NAVIO", "Navio e embarque"),
            Map.entry("NAVIOEEMBARQUE", "Navio e embarque"),
            Map.entry("EMBARQUE", "Navio e embarque"),
            Map.entry("CONTROLROOM", "Control Room"),
            Map.entry("FERROVIA", "Ferrovia"),
            Map.entry("PATIO", "Pátio"),
            Map.entry("CARGAGERAL", "Carga geral"),
            Map.entry("FATURAMENTO", "Faturamento"),
            Map.entry("PORTALDOCLIENTE", "Portal do cliente"),
            Map.entry("PORTALCLIENTE", "Portal do cliente"),
            Map.entry("INTEGRACOES", "Integrações")
    );

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
            return "Outros";
        }
        String chave = Normalizer.normalize(texto, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase(Locale.ROOT);
        return ROTULOS_GRUPO.getOrDefault(chave, formatarGrupo(texto));
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

    private String formatarGrupo(String texto) {
        String legivel = texto.replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (legivel.isEmpty()) {
            return "Outros";
        }
        return Character.toUpperCase(legivel.charAt(0)) + legivel.substring(1);
    }

    private String sanitizarTextoBasico(String valor) {
        if (valor == null) {
            return "";
        }
        String decodificado = valor;
        for (int tentativa = 0; tentativa < 3; tentativa++) {
            String anterior = decodificado;
            decodificado = HtmlUtils.htmlUnescape(decodificado);
            if (anterior.equals(decodificado)) {
                break;
            }
        }
        String normalizado = Normalizer.normalize(decodificado, Normalizer.Form.NFKC).trim();
        String semMarcacao = normalizado.replaceAll("<[^>]*>", "")
                .replace("<", "")
                .replace(">", "");
        return semMarcacao.replaceAll("[\\p{Cntrl}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
