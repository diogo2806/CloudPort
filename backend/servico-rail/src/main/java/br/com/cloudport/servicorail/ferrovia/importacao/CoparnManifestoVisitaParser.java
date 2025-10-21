package br.com.cloudport.servicorail.ferrovia.importacao;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.comum.validacao.ValidacaoEntradaUtil;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CoparnManifestoVisitaParser implements ArquivoManifestoVisitaParser {

    private static final DateTimeFormatter FORMATO_CCYMMDDHHMM = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final DateTimeFormatter FORMATO_CCYMMDDHH = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final SanitizadorEntrada sanitizadorEntrada;

    public CoparnManifestoVisitaParser(SanitizadorEntrada sanitizadorEntrada) {
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Override
    public boolean suporta(String nomeArquivo, byte[] conteudo) {
        if (nomeArquivo != null) {
            String nome = nomeArquivo.toLowerCase(Locale.ROOT);
            if (nome.endsWith(".edi") || nome.endsWith(".edifact") || nome.endsWith(".ifc")) {
                return true;
            }
        }
        if (conteudo == null || conteudo.length == 0) {
            return false;
        }
        String texto = new String(conteudo, StandardCharsets.UTF_8);
        String textoNormalizado = texto.toUpperCase(Locale.ROOT);
        return textoNormalizado.contains("UNH+") && textoNormalizado.contains("COPARN");
    }

    @Override
    public ResultadoManifestoVisita parse(String nomeArquivo, byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo COPARN está vazio.");
        }
        String mensagem = new String(conteudo, StandardCharsets.UTF_8);
        String[] segmentos = mensagem.split("'");
        ResultadoManifestoVisita resultado = new ResultadoManifestoVisita();

        String ultimoConteiner = null;
        TipoOperacaoManifesto ultimaOperacao = TipoOperacaoManifesto.DESCARGA;
        int contadorVagao = 0;

        for (String segmentoBruto : segmentos) {
            String segmento = segmentoBruto.trim();
            if (!StringUtils.hasText(segmento)) {
                continue;
            }
            String[] partes = segmento.split("\\+");
            String codigo = partes[0].toUpperCase(Locale.ROOT);
            switch (codigo) {
                case "TDT":
                    tratarSegmentoTdt(resultado, partes);
                    break;
                case "NAD":
                    tratarSegmentoNad(resultado, partes);
                    break;
                case "DTM":
                    tratarSegmentoDtm(resultado, partes);
                    break;
                case "EQD":
                    if (ultimoConteiner != null) {
                        adicionarConteiner(resultado, ultimoConteiner, ultimaOperacao);
                    }
                    ultimoConteiner = extrairIdentificador(partes, 2);
                    ultimaOperacao = TipoOperacaoManifesto.DESCARGA;
                    break;
                case "FTX":
                    if (ultimoConteiner != null) {
                        ultimaOperacao = identificarOperacao(segmento, ultimaOperacao);
                    }
                    break;
                case "LOC":
                    if (partes.length > 1 && "147".equals(partes[1])) {
                        contadorVagao++;
                        String identificadorVagao = extrairIdentificador(partes, 2);
                        String tipoVagao = extrairIdentificador(partes, 3);
                        if (StringUtils.hasText(identificadorVagao)) {
                            resultado.adicionarVagao(contadorVagao, identificadorVagao, tipoVagao);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        if (ultimoConteiner != null) {
            adicionarConteiner(resultado, ultimoConteiner, ultimaOperacao);
        }

        validarObrigatorios(resultado);
        return resultado;
    }

    private void tratarSegmentoTdt(ResultadoManifestoVisita resultado, String[] partes) {
        if (partes.length > 2 && "20".equals(partes[1])) {
            String identificador = extrairIdentificador(partes, 2);
            if (StringUtils.hasText(identificador)) {
                resultado.setIdentificadorTrem(identificador);
            }
        }
    }

    private void tratarSegmentoNad(ResultadoManifestoVisita resultado, String[] partes) {
        if (partes.length > 2) {
            String papel = partes[1].toUpperCase(Locale.ROOT);
            if (("MS".equals(papel) || "CA".equals(papel)) && !StringUtils.hasText(resultado.getOperadoraFerroviaria())) {
                resultado.setOperadoraFerroviaria(extrairIdentificador(partes, 2));
            }
        }
    }

    private void tratarSegmentoDtm(ResultadoManifestoVisita resultado, String[] partes) {
        if (partes.length < 2) {
            return;
        }
        String[] componentes = partes[1].split(":");
        if (componentes.length < 2) {
            return;
        }
        String codigo = componentes[0];
        String valorData = componentes[1];
        LocalDateTime data = interpretarData(valorData);
        if (data == null) {
            return;
        }
        switch (codigo) {
            case "132":
                resultado.setHoraChegadaPrevista(data);
                break;
            case "133":
                resultado.setHoraPartidaPrevista(data);
                break;
            case "137":
                resultado.setStatusVisita(StatusVisitaTrem.PROCESSANDO);
                break;
            default:
                break;
        }
    }

    private void adicionarConteiner(ResultadoManifestoVisita resultado,
                                    String identificador,
                                    TipoOperacaoManifesto operacao) {
        if (!StringUtils.hasText(identificador)) {
            return;
        }
        if (operacao == TipoOperacaoManifesto.CARGA) {
            resultado.adicionarConteinerCarga(identificador);
        } else {
            resultado.adicionarConteinerDescarga(identificador);
        }
    }

    private TipoOperacaoManifesto identificarOperacao(String segmento, TipoOperacaoManifesto atual) {
        String texto = segmento.toUpperCase(Locale.ROOT);
        if (texto.contains("EXP") || texto.contains("LOAD") || texto.contains("EXPORT")) {
            return TipoOperacaoManifesto.CARGA;
        }
        if (texto.contains("IMP") || texto.contains("DIS") || texto.contains("UNLOAD") || texto.contains("IMPORT")) {
            return TipoOperacaoManifesto.DESCARGA;
        }
        return atual;
    }

    private void validarObrigatorios(ResultadoManifestoVisita resultado) {
        if (!StringUtils.hasText(resultado.getIdentificadorTrem())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo COPARN não informa o identificador do trem.");
        }
        if (!StringUtils.hasText(resultado.getOperadoraFerroviaria())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo COPARN não informa a operadora ferroviária.");
        }
        if (resultado.getHoraChegadaPrevista() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O arquivo COPARN não informa a hora prevista de chegada.");
        }
    }

    private String extrairIdentificador(String[] partes, int indice) {
        if (partes.length <= indice) {
            return null;
        }
        String valor = partes[indice];
        int separador = valor.indexOf(':');
        String semSufixo = separador >= 0 ? valor.substring(0, separador) : valor;
        String limpo = sanitizadorEntrada.limparTexto(semSufixo);
        limpo = ValidacaoEntradaUtil.limparTexto(limpo);
        if (!StringUtils.hasText(limpo)) {
            return null;
        }
        return limpo.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime interpretarData(String valor) {
        String texto = sanitizadorEntrada.limparTexto(valor);
        texto = ValidacaoEntradaUtil.limparTexto(texto);
        if (!StringUtils.hasText(texto)) {
            return null;
        }
        String normalizado = texto.trim();
        try {
            if (normalizado.length() == 12) {
                return LocalDateTime.parse(normalizado, FORMATO_CCYMMDDHHMM);
            }
            if (normalizado.length() == 10) {
                return LocalDateTime.parse(normalizado, FORMATO_CCYMMDDHH);
            }
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.format(Locale.ROOT, "Não foi possível interpretar a data '%s'.", normalizado));
        }
        return null;
    }

    private enum TipoOperacaoManifesto {
        DESCARGA,
        CARGA
    }
}
