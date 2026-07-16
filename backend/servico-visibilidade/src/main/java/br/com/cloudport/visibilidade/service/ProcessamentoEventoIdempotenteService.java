package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.EventoProcessado;
import br.com.cloudport.visibilidade.exception.ConflitoIdentidadeEventoException;
import br.com.cloudport.visibilidade.repository.EventoProcessadoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessamentoEventoIdempotenteService {

    private static final int TAMANHO_MAXIMO_IDENTIDADE = 150;
    private static final int TAMANHO_MAXIMO_TIPO = 100;

    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final ObjectWriter escritorCanonico;

    public ProcessamentoEventoIdempotenteService(EventoProcessadoRepository eventoProcessadoRepository,
                                                  ObjectMapper objectMapper) {
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        ObjectMapper mapperCanonico = objectMapper.copy();
        mapperCanonico.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        mapperCanonico.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        this.escritorCanonico = mapperCanonico.writer();
    }

    @Transactional
    public boolean processarUmaVez(Map<String, Object> evento, Consumer<String> processamento) {
        if (processamento == null) {
            throw new IllegalArgumentException("O processamento do evento e obrigatorio.");
        }

        String identidadeEvento = extrairIdentidade(evento);
        String tipoEvento = extrairTipo(evento);
        String hashPayload = calcularHashPayload(evento);

        int inseridos = eventoProcessadoRepository.inserirSeAusente(
                identidadeEvento, tipoEvento, hashPayload);
        if (inseridos == 1) {
            processamento.accept(identidadeEvento);
            return true;
        }
        if (inseridos != 0) {
            throw new IllegalStateException("Quantidade inesperada ao registrar identidade de evento: " + inseridos);
        }

        EventoProcessado existente = eventoProcessadoRepository.findById(identidadeEvento)
                .orElseThrow(() -> new IllegalStateException(
                        "A identidade do evento apresentou conflito, mas o registro persistido nao foi encontrado."));
        validarRedelivery(existente, tipoEvento, hashPayload);
        return false;
    }

    private String extrairIdentidade(Map<String, Object> evento) {
        if (evento == null) {
            throw new IllegalArgumentException("O envelope do evento nao pode ser nulo.");
        }

        String eventId = texto(evento, "eventId");
        String messageId = texto(evento, "messageId");
        if (!StringUtils.hasText(eventId) && !StringUtils.hasText(messageId)) {
            throw new IllegalArgumentException("eventId ou messageId e obrigatorio no envelope do evento.");
        }
        if (StringUtils.hasText(eventId)
                && StringUtils.hasText(messageId)
                && !eventId.equals(messageId)) {
            throw new ConflitoIdentidadeEventoException(
                    "eventId e messageId informados no mesmo envelope possuem valores diferentes.");
        }

        String identidade = StringUtils.hasText(eventId) ? eventId : messageId;
        if (identidade.length() > TAMANHO_MAXIMO_IDENTIDADE) {
            throw new IllegalArgumentException("A identidade do evento excede 150 caracteres.");
        }
        return identidade;
    }

    private String extrairTipo(Map<String, Object> evento) {
        String tipoEvento = texto(evento, "eventType");
        if (!StringUtils.hasText(tipoEvento)) {
            throw new IllegalArgumentException("eventType e obrigatorio no envelope do evento.");
        }
        if (tipoEvento.length() > TAMANHO_MAXIMO_TIPO) {
            throw new IllegalArgumentException("eventType excede 100 caracteres.");
        }
        return tipoEvento;
    }

    private String calcularHashPayload(Map<String, Object> evento) {
        Map<String, Object> payloadCanonico = new TreeMap<>(evento);
        payloadCanonico.remove("eventId");
        payloadCanonico.remove("messageId");

        try {
            byte[] json = escritorCanonico.writeValueAsString(payloadCanonico)
                    .getBytes(StandardCharsets.UTF_8);
            return paraHexadecimal(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Nao foi possivel serializar o payload do evento.", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao esta disponivel no runtime.", ex);
        }
    }

    private void validarRedelivery(EventoProcessado existente,
                                   String tipoEvento,
                                   String hashPayload) {
        if (!Objects.equals(existente.getTipoEvento(), tipoEvento)
                || !Objects.equals(existente.getHashPayload(), hashPayload)) {
            throw new ConflitoIdentidadeEventoException(
                    "A identidade " + existente.getIdentidadeEvento()
                            + " ja foi processada com tipo ou payload diferente.");
        }
    }

    private String texto(Map<String, Object> evento, String chave) {
        Object valor = evento.get(chave);
        return valor == null ? null : String.valueOf(valor).trim();
    }

    private String paraHexadecimal(byte[] bytes) {
        StringBuilder hexadecimal = new StringBuilder(bytes.length * 2);
        for (byte valor : bytes) {
            hexadecimal.append(String.format("%02x", valor & 0xff));
        }
        return hexadecimal.toString();
    }
}
