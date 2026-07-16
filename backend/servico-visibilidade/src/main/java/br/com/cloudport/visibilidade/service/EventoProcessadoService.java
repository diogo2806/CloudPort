package br.com.cloudport.visibilidade.service;

import br.com.cloudport.visibilidade.dto.evento.EventoRecebido;
import br.com.cloudport.visibilidade.entity.EventoProcessado;
import br.com.cloudport.visibilidade.exception.EventoEnvelopeInvalidoException;
import br.com.cloudport.visibilidade.exception.EventoIdentidadeColidenteException;
import br.com.cloudport.visibilidade.repository.EventoProcessadoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EventoProcessadoService {

    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final ObjectMapper objectMapperCanonico;

    public EventoProcessadoService(EventoProcessadoRepository eventoProcessadoRepository,
                                   ObjectMapper objectMapper) {
        this.eventoProcessadoRepository = eventoProcessadoRepository;
        this.objectMapperCanonico = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    @Transactional
    public boolean processarUmaVez(String consumidor,
                                   Map<String, Object> envelope,
                                   Consumer<EventoRecebido> processador) {
        if (!StringUtils.hasText(consumidor)) {
            throw new EventoEnvelopeInvalidoException("O consumidor do evento e obrigatorio.");
        }
        if (processador == null) {
            throw new EventoEnvelopeInvalidoException("O processador do evento e obrigatorio.");
        }

        EventoRecebido evento = EventoRecebido.de(envelope);
        String hashPayload = calcularHash(evento.getEnvelope());
        Optional<EventoProcessado> existente = eventoProcessadoRepository
                .findByIdentidadeEvento(evento.getIdentidade());

        if (existente.isPresent()) {
            validarRedelivery(existente.get(), evento, hashPayload);
            return false;
        }

        EventoProcessado registro = criarRegistro(consumidor, evento, hashPayload);
        eventoProcessadoRepository.saveAndFlush(registro);
        processador.accept(evento);
        return true;
    }

    private EventoProcessado criarRegistro(String consumidor,
                                            EventoRecebido evento,
                                            String hashPayload) {
        EventoProcessado registro = new EventoProcessado();
        registro.setIdentidadeEvento(evento.getIdentidade());
        registro.setTipoEvento(evento.getTipo());
        registro.setVersaoEvento(evento.getVersao());
        registro.setConsumidor(consumidor.trim());
        registro.setOrigemEvento(normalizar(evento.getOrigem()));
        registro.setHashPayload(hashPayload);
        registro.setProcessadoEm(LocalDateTime.now());
        return registro;
    }

    private void validarRedelivery(EventoProcessado existente,
                                   EventoRecebido evento,
                                   String hashPayload) {
        if (!hashPayload.equals(existente.getHashPayload())) {
            throw new EventoIdentidadeColidenteException(
                    "A identidade " + evento.getIdentidade()
                            + " ja foi processada com um payload diferente.");
        }
    }

    private String calcularHash(Map<String, Object> envelope) {
        try {
            byte[] payload = objectMapperCanonico.writeValueAsBytes(envelope);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return converterHexadecimal(digest.digest(payload));
        } catch (JsonProcessingException ex) {
            throw new EventoEnvelopeInvalidoException(
                    "Nao foi possivel serializar o envelope do evento.");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao esta disponivel no runtime.", ex);
        }
    }

    private String converterHexadecimal(byte[] bytes) {
        StringBuilder hexadecimal = new StringBuilder(bytes.length * 2);
        for (byte valor : bytes) {
            hexadecimal.append(String.format("%02x", valor & 0xff));
        }
        return hexadecimal.toString();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
