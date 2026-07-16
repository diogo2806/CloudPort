package br.com.cloudport.visibilidade.dto.evento;

import br.com.cloudport.visibilidade.exception.EventoEnvelopeInvalidoException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class EventoRecebido {

    private final Map<String, Object> envelope;
    private final Map<String, Object> dados;
    private final String identidade;
    private final String tipo;
    private final int versao;
    private final String origem;

    private EventoRecebido(Map<String, Object> envelope,
                           Map<String, Object> dados,
                           String identidade,
                           String tipo,
                           int versao,
                           String origem) {
        this.envelope = envelope;
        this.dados = dados;
        this.identidade = identidade;
        this.tipo = tipo;
        this.versao = versao;
        this.origem = origem;
    }

    public static EventoRecebido de(Map<String, Object> envelopeRecebido) {
        if (envelopeRecebido == null || envelopeRecebido.isEmpty()) {
            throw new EventoEnvelopeInvalidoException("O envelope do evento nao pode ser vazio.");
        }

        Map<String, Object> envelope = Collections.unmodifiableMap(
                new LinkedHashMap<>(envelopeRecebido));
        String identidade = primeiroTexto(envelope, "eventId", "messageId");
        if (!StringUtils.hasText(identidade)) {
            throw new EventoEnvelopeInvalidoException(
                    "O evento deve informar eventId ou messageId.");
        }

        String tipo = texto(envelope, "eventType");
        if (!StringUtils.hasText(tipo)) {
            throw new EventoEnvelopeInvalidoException("O evento deve informar eventType.");
        }

        Integer versao = primeiroInteiro(envelope, "eventVersion", "version");
        if (versao == null || versao < 1) {
            throw new EventoEnvelopeInvalidoException(
                    "O evento deve informar eventVersion maior que zero.");
        }

        Map<String, Object> dados = extrairDados(envelope.get("data"));
        String origem = primeiroTexto(envelope, "source", "origem");
        return new EventoRecebido(envelope, dados, identidade, tipo, versao, origem);
    }

    public Object valor(String chave) {
        if (dados.containsKey(chave)) {
            return dados.get(chave);
        }
        return envelope.get(chave);
    }

    public String texto(String chave) {
        Object valor = valor(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }

    public String primeiroTexto(String... chaves) {
        for (String chave : chaves) {
            String valor = texto(chave);
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }

    public Integer inteiro(String chave) {
        Object valor = valor(chave);
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(valor).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Long longo(String chave) {
        Object valor = valor(chave);
        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(valor).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Map<String, Object> getEnvelope() {
        return envelope;
    }

    public String getIdentidade() {
        return identidade;
    }

    public String getTipo() {
        return tipo;
    }

    public int getVersao() {
        return versao;
    }

    public String getOrigem() {
        return origem;
    }

    private static Map<String, Object> extrairDados(Object dadosBrutos) {
        if (!(dadosBrutos instanceof Map<?, ?>)) {
            return Collections.emptyMap();
        }

        Map<String, Object> dados = new LinkedHashMap<>();
        Map<?, ?> mapa = (Map<?, ?>) dadosBrutos;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (entrada.getKey() != null) {
                dados.put(String.valueOf(entrada.getKey()), entrada.getValue());
            }
        }
        return Collections.unmodifiableMap(dados);
    }

    private static String primeiroTexto(Map<String, Object> mapa, String... chaves) {
        for (String chave : chaves) {
            String valor = texto(mapa, chave);
            if (StringUtils.hasText(valor)) {
                return valor;
            }
        }
        return null;
    }

    private static String texto(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }

    private static Integer primeiroInteiro(Map<String, Object> mapa, String... chaves) {
        for (String chave : chaves) {
            Object valor = mapa.get(chave);
            if (valor instanceof Number) {
                return ((Number) valor).intValue();
            }
            if (valor != null) {
                try {
                    return Integer.valueOf(String.valueOf(valor).trim());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }
}
