package br.com.cloudport.servicorail.ferrovia.importacao;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CsvManifestoVisitaParser implements ArquivoManifestoVisitaParser {

    private static final DateTimeFormatter FORMATO_ISO = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter FORMATO_PADRAO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SanitizadorEntrada sanitizadorEntrada;

    public CsvManifestoVisitaParser(SanitizadorEntrada sanitizadorEntrada) {
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Override
    public boolean suporta(String nomeArquivo, byte[] conteudo) {
        if (nomeArquivo != null && nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return true;
        }
        if (conteudo == null || conteudo.length == 0) {
            return false;
        }
        String linhaInicial = obterPrimeiraLinha(conteudo);
        return linhaInicial.contains(";") || linhaInicial.contains(",");
    }

    @Override
    public ResultadoManifestoVisita parse(String nomeArquivo, byte[] conteudo) {
        ResultadoManifestoVisita resultado = new ResultadoManifestoVisita();
        List<String> linhas = lerLinhas(conteudo);
        if (linhas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo CSV está vazio.");
        }
        String delimitador = linhas.get(0).contains(";") ? ";" : ",";
        Pattern separador = Pattern.compile(Pattern.quote(delimitador));
        int indiceVagao = 0;

        for (String linha : linhas) {
            if (!StringUtils.hasText(linha) || linha.trim().startsWith("#")) {
                continue;
            }
            String[] colunas = separador.split(linha, -1);
            if (colunas.length == 0) {
                continue;
            }
            String tipoRegistro = normalizarTexto(colunas[0]);
            if (!StringUtils.hasText(tipoRegistro)) {
                continue;
            }
            if ("TIPO".equals(tipoRegistro) || "REGISTRO".equals(tipoRegistro)) {
                continue;
            }
            switch (tipoRegistro) {
                case "VISITA":
                    preencherDadosVisita(resultado, colunas);
                    break;
                case "CONTEINER_DESCARGA":
                case "MANIFESTO_DESCARGA":
                    adicionarConteineres(resultado::adicionarConteinerDescarga, colunas);
                    break;
                case "CONTEINER_CARGA":
                case "MANIFESTO_CARGA":
                    adicionarConteineres(resultado::adicionarConteinerCarga, colunas);
                    break;
                case "VAGAO":
                    indiceVagao = adicionarVagao(resultado, colunas, indiceVagao);
                    break;
                default:
                    break;
            }
        }

        validarObrigatorios(resultado);
        return resultado;
    }

    private String obterPrimeiraLinha(byte[] conteudo) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8))) {
            String linha = reader.readLine();
            return linha != null ? linha : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private List<String> lerLinhas(byte[] conteudo) {
        List<String> linhas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                linhas.add(linha.trim());
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível ler o arquivo CSV informado.");
        }
        return linhas;
    }

    private void preencherDadosVisita(ResultadoManifestoVisita resultado, String[] colunas) {
        if (colunas.length > 1) {
            resultado.setIdentificadorTrem(normalizarTexto(colunas[1]));
        }
        if (colunas.length > 2) {
            resultado.setOperadoraFerroviaria(normalizarTexto(colunas[2]));
        }
        if (colunas.length > 3) {
            resultado.setHoraChegadaPrevista(converterData(colunas[3]));
        }
        if (colunas.length > 4) {
            resultado.setHoraPartidaPrevista(converterData(colunas[4]));
        }
        if (colunas.length > 5) {
            String statusTexto = normalizarTexto(colunas[5]);
            if (StringUtils.hasText(statusTexto)) {
                try {
                    resultado.setStatusVisita(StatusVisitaTrem.valueOf(statusTexto.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    resultado.setStatusVisita(StatusVisitaTrem.PLANEJADO);
                }
            }
        }
    }

    private void adicionarConteineres(java.util.function.Consumer<String> consumidor, String[] colunas) {
        for (int i = 1; i < colunas.length; i++) {
            String identificacao = normalizarTexto(colunas[i]);
            if (StringUtils.hasText(identificacao)) {
                consumidor.accept(identificacao);
            }
        }
    }

    private int adicionarVagao(ResultadoManifestoVisita resultado, String[] colunas, int indiceAtual) {
        int proximaPosicao = indiceAtual + 1;
        Integer posicaoInformada = null;
        if (colunas.length > 1) {
            try {
                posicaoInformada = Integer.parseInt(colunas[1].trim());
            } catch (NumberFormatException ex) {
                posicaoInformada = null;
            }
        }
        Integer posicaoFinal = posicaoInformada != null && posicaoInformada > 0 ? posicaoInformada : proximaPosicao;
        String identificador = colunas.length > 2 ? normalizarTexto(colunas[2]) : null;
        String tipo = colunas.length > 3 ? normalizarTexto(colunas[3]) : null;
        if (StringUtils.hasText(identificador)) {
            resultado.adicionarVagao(posicaoFinal, identificador, tipo);
            if (posicaoInformada == null || posicaoInformada <= 0) {
                return posicaoFinal;
            }
        }
        return indiceAtual;
    }

    private void validarObrigatorios(ResultadoManifestoVisita resultado) {
        if (!StringUtils.hasText(resultado.getIdentificadorTrem())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo CSV não informa o identificador do trem.");
        }
        if (!StringUtils.hasText(resultado.getOperadoraFerroviaria())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo CSV não informa a operadora ferroviária.");
        }
        if (resultado.getHoraChegadaPrevista() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo CSV não informa a hora prevista de chegada do trem.");
        }
    }

    private LocalDateTime converterData(String valor) {
        String textoLimpo = normalizarTexto(valor);
        if (!StringUtils.hasText(textoLimpo)) {
            return null;
        }
        try {
            return LocalDateTime.parse(textoLimpo, FORMATO_ISO);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(textoLimpo, FORMATO_PADRAO);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format(Locale.ROOT, "Não foi possível interpretar a data '%s'.", textoLimpo));
        }
    }

    private String normalizarTexto(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        if (!StringUtils.hasText(limpo)) {
            return null;
        }
        return limpo.trim();
    }
}
