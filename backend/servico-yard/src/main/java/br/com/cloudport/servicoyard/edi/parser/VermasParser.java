package br.com.cloudport.servicoyard.edi.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class VermasParser {

    public ResultadoVermas parse(String conteudoEdifact) {
        if (!StringUtils.hasText(conteudoEdifact)) {
            throw new IllegalArgumentException("VERMAS: conteudo EDIFACT vazio.");
        }
        String normalizado = conteudoEdifact.replace("\r", "").replace("\n", "").trim();
        if (!normalizado.contains("UNH+") || !normalizado.toUpperCase(Locale.ROOT).contains("VERMAS")) {
            throw new IllegalArgumentException("VERMAS: cabecalho UNH com tipo VERMAS nao encontrado.");
        }

        String codigoNavio = null;
        String codigoViagem = null;
        String containerAtual = null;
        List<PesoVgm> pesos = new ArrayList<>();

        for (String segmentoBruto : normalizado.split("'")) {
            String segmento = segmentoBruto.trim();
            if (segmento.startsWith("TDT+")) {
                String[] elementos = segmento.split("\\+", -1);
                if (elementos.length > 2 && StringUtils.hasText(elementos[2])) {
                    codigoViagem = primeiroComponente(elementos[2]);
                }
                for (int indice = elementos.length - 1; indice >= 0; indice--) {
                    String candidato = primeiroComponente(elementos[indice]);
                    if (StringUtils.hasText(candidato) && candidato.matches("[A-Za-z0-9._-]{3,50}")) {
                        codigoNavio = candidato;
                        break;
                    }
                }
            } else if (segmento.startsWith("EQD+CN+")) {
                String[] elementos = segmento.split("\\+", -1);
                containerAtual = elementos.length > 2 ? normalizarContainer(primeiroComponente(elementos[2])) : null;
            } else if (segmento.startsWith("MEA+") && segmento.contains("VGM")) {
                if (!StringUtils.hasText(containerAtual)) {
                    throw new IllegalArgumentException("VERMAS: segmento VGM sem container EQD associado.");
                }
                Double pesoKg = extrairPesoKg(segmento);
                pesos.add(new PesoVgm(containerAtual, pesoKg));
                containerAtual = null;
            }
        }

        if (pesos.isEmpty()) {
            throw new IllegalArgumentException("VERMAS: nenhum peso bruto verificado foi encontrado.");
        }
        return new ResultadoVermas(codigoNavio, codigoViagem, List.copyOf(pesos));
    }

    private Double extrairPesoKg(String segmento) {
        String[] elementos = segmento.split("\\+", -1);
        String unidade = "KGM";
        Double valor = null;
        for (String elemento : elementos) {
            String[] componentes = elemento.split(":", -1);
            for (int indice = 0; indice < componentes.length; indice++) {
                String componente = componentes[indice].trim().toUpperCase(Locale.ROOT);
                if ("KGM".equals(componente) || "TNE".equals(componente)) {
                    unidade = componente;
                    if (indice + 1 < componentes.length) {
                        valor = numero(componentes[indice + 1]);
                    }
                } else if (valor == null && componente.matches("\\d+(?:[.,]\\d+)?")) {
                    valor = numero(componente);
                }
            }
        }
        if (valor == null || valor <= 0) {
            throw new IllegalArgumentException("VERMAS: peso VGM invalido no segmento " + segmento + ".");
        }
        return "TNE".equals(unidade) ? valor * 1000D : valor;
    }

    private Double numero(String valor) {
        try {
            return Double.valueOf(valor.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("VERMAS: peso VGM nao numerico: " + valor + ".", ex);
        }
    }

    private String primeiroComponente(String valor) {
        if (valor == null) {
            return null;
        }
        int separador = valor.indexOf(':');
        return (separador >= 0 ? valor.substring(0, separador) : valor).trim();
    }

    private String normalizarContainer(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalizado.length() < 10 || normalizado.length() > 12) {
            throw new IllegalArgumentException("VERMAS: identificador de container invalido: " + valor + ".");
        }
        return normalizado;
    }

    public record PesoVgm(String codigoContainer, Double pesoKg) {
    }

    public record ResultadoVermas(String codigoNavio, String codigoViagem, List<PesoVgm> pesos) {
    }
}
