package br.com.cloudport.servicogate.integration.tos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

final class TosObservabilidadeSegura {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final int TAMANHO_MAXIMO_CORPO_ANALISADO = 4096;
    private static final int TAMANHO_MAXIMO_CORRELATION_ID = 128;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern CODIGO_ERRO_PERMITIDO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private static final Pattern CORRELATION_ID_PERMITIDO = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern CARACTERES_IDENTIFICADOR = Pattern.compile("[^A-Za-z0-9._-]");
    private static final List<String> CAMPOS_CODIGO_ERRO = List.of(
            "code",
            "codigo",
            "errorCode",
            "error_code"
    );

    private TosObservabilidadeSegura() {
    }

    static String resumirErroSeguro(WebClientResponseException exception) {
        String codigoPadrao = "TOS_HTTP_" + exception.getRawStatusCode();
        String corpo = exception.getResponseBodyAsString();
        if (!StringUtils.hasText(corpo) || corpo.length() > TAMANHO_MAXIMO_CORPO_ANALISADO) {
            return codigoPadrao;
        }

        try {
            JsonNode raiz = OBJECT_MAPPER.readTree(corpo);
            if (raiz == null || !raiz.isObject()) {
                return codigoPadrao;
            }
            for (String campo : CAMPOS_CODIGO_ERRO) {
                JsonNode valor = raiz.get(campo);
                if (valor != null && valor.isTextual()) {
                    String codigo = valor.asText().trim();
                    if (CODIGO_ERRO_PERMITIDO.matcher(codigo).matches()) {
                        return codigo;
                    }
                }
            }
        } catch (JsonProcessingException ignored) {
            return codigoPadrao;
        }
        return codigoPadrao;
    }

    static String mascararIdentificador(String identificador) {
        if (!StringUtils.hasText(identificador)) {
            return "ausente";
        }
        String normalizado = CARACTERES_IDENTIFICADOR.matcher(identificador.trim()).replaceAll("");
        if (!StringUtils.hasText(normalizado)) {
            return "ausente";
        }
        if (normalizado.length() <= 4) {
            return "***";
        }
        return normalizado.substring(0, 2) + "***" + normalizado.substring(normalizado.length() - 2);
    }

    static String obterCorrelationId() {
        return normalizarCorrelationId(MDC.get(MDC_CORRELATION_ID));
    }

    static String normalizarCorrelationId(String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            String normalizado = correlationId.trim();
            if (normalizado.length() <= TAMANHO_MAXIMO_CORRELATION_ID
                    && CORRELATION_ID_PERMITIDO.matcher(normalizado).matches()) {
                return normalizado;
            }
        }
        return UUID.randomUUID().toString();
    }
}
