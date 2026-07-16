package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.entity.EventoConsumido;
import br.com.cloudport.visibilidade.repository.EventoConsumidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProcessamentoEventoIdempotenteService {

    private final EventoConsumidoRepository eventoConsumidoRepository;
    private final ObjectMapper objectMapper;

    public ProcessamentoEventoIdempotenteService(EventoConsumidoRepository eventoConsumidoRepository,
                                                  ObjectMapper objectMapper) {
        this.eventoConsumidoRepository = eventoConsumidoRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean processarUmaVez(String origem,
                                   String tipoEvento,
                                   Map<String, Object> evento,
                                   Consumer<String> efeito) {
        String eventoId = extrairIdentidade(evento);
        String origemNormalizada = obrigatorio(origem, "origem");
        String tipoNormalizado = obrigatorio(tipoEvento, "tipoEvento");
        String hashPayload = calcularHash(evento);

        Optional<EventoConsumido> existente = eventoConsumidoRepository.findById(eventoId);
        if (existente.isPresent()) {
            validarRedelivery(existente.get(), origemNormalizada, tipoNormalizado, hashPayload);
            return false;
        }

        EventoConsumido consumido = new EventoConsumido();
        consumido.setEventoId(eventoId);
        consumido.setOrigem(origemNormalizada);
        consumido.setTipoEvento(tipoNormalizado);
        consumido.setHashPayload(hashPayload);
        eventoConsumidoRepository.saveAndFlush(consumido);

        efeito.accept(eventoId);
        return true;
    }

    private String extrairIdentidade(Map<String, Object> evento) {
        if (evento == null) {
            throw new IllegalArgumentException("O envelope do evento nao pode ser nulo.");
        }
        String eventId = texto(evento.get("eventId"));
        String messageId = texto(evento.get("messageId"));
        if (StringUtils.hasText(eventId) && StringUtils.hasText(messageId)
                && !eventId.equals(messageId)) {
            throw new IllegalArgumentException(
                    "eventId e messageId divergentes no mesmo envelope de evento.");
        }
        String identidade = StringUtils.hasText(eventId) ? eventId : messageId;
        if (!StringUtils.hasText(identidade)) {
            throw new IllegalArgumentException(
                    "O envelope do evento deve informar eventId ou messageId.");
        }
        if (identidade.length() > 150) {
            throw new IllegalArgumentException("A identidade do evento excede 150 caracteres.");
        }
        return identidade;
    }

    private void validarRedelivery(EventoConsumido existente,
                                    String origem,
                                    String tipoEvento,
                                    String hashPayload) {
        if (!origem.equals(existente.getOrigem())
                || !tipoEvento.equals(existente.getTipoEvento())
                || !hashPayload.equals(existente.getHashPayload())) {
            throw new IllegalStateException(
                    "Colisao de identidade detectada para o evento " + existente.getEventoId()
                            + ": o payload recebido diverge do evento ja processado.");
        }
    }

    private String calcularHash(Map<String, Object> evento) {
        try {
            byte[] payloadCanonico = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsBytes(evento);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payloadCanonico));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Nao foi possivel serializar o envelope do evento.", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel no runtime Java.", ex);
        }
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(campo + " e obrigatorio para processar o evento.");
        }
        return valor.trim();
    }

    private String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor).trim();
    }
}
