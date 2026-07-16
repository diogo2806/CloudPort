package br.com.cloudport.servicoyard.edi.servico;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EdiIdentificadorExtrator {

    private static final char SEPARADOR_ELEMENTO_PADRAO = '+';
    private static final char TERMINADOR_SEGMENTO_PADRAO = '\'';
    private static final char CARACTERE_LIBERACAO_PADRAO = '?';

    public IdentificadoresEdi extrair(String conteudoEdifact) {
        if (!StringUtils.hasText(conteudoEdifact)) {
            return new IdentificadoresEdi(null, null);
        }

        String conteudo = removerBom(conteudoEdifact).trim();
        char separadorElemento = SEPARADOR_ELEMENTO_PADRAO;
        char terminadorSegmento = TERMINADOR_SEGMENTO_PADRAO;
        char caractereLiberacao = CARACTERE_LIBERACAO_PADRAO;
        if (conteudo.startsWith("UNA") && conteudo.length() >= 9) {
            separadorElemento = conteudo.charAt(4);
            caractereLiberacao = conteudo.charAt(6);
            terminadorSegmento = conteudo.charAt(8);
        }

        String interchange = null;
        String mensagem = null;
        List<String> segmentos = separar(conteudo, terminadorSegmento, caractereLiberacao);
        if (segmentos.size() == 1 && conteudo.indexOf(terminadorSegmento) < 0) {
            segmentos = List.of(conteudo.split("\\R"));
        }

        for (String segmentoOriginal : segmentos) {
            String segmento = segmentoOriginal.replace("\r", "").replace("\n", "").trim();
            if (!StringUtils.hasText(segmento) || segmento.startsWith("UNA")) {
                continue;
            }
            List<String> elementos = separar(segmento, separadorElemento, caractereLiberacao);
            if (elementos.isEmpty()) {
                continue;
            }
            String tag = elementos.get(0).trim().toUpperCase(Locale.ROOT);
            if ("UNB".equals(tag) && elementos.size() > 5) {
                interchange = normalizar(elementos.get(5));
            } else if ("UNH".equals(tag) && elementos.size() > 1) {
                mensagem = normalizar(elementos.get(1));
            }
            if (interchange != null && mensagem != null) {
                break;
            }
        }
        return new IdentificadoresEdi(interchange, mensagem);
    }

    private List<String> separar(String valor, char separador, char caractereLiberacao) {
        List<String> partes = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean liberado = false;
        for (int indice = 0; indice < valor.length(); indice++) {
            char caractere = valor.charAt(indice);
            if (liberado) {
                atual.append(caractere);
                liberado = false;
            } else if (caractere == caractereLiberacao) {
                liberado = true;
            } else if (caractere == separador) {
                partes.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(caractere);
            }
        }
        partes.add(atual.toString());
        return partes;
    }

    private String removerBom(String valor) {
        return valor.startsWith("\uFEFF") ? valor.substring(1) : valor;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }
}
