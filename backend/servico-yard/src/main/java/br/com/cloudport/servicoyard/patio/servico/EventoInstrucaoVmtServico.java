package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.modelo.EventoInstrucaoVmt;
import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import br.com.cloudport.servicoyard.patio.repositorio.EventoInstrucaoVmtRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.InstrucaoTrabalhoRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class EventoInstrucaoVmtServico {

    private final EventoInstrucaoVmtRepositorio eventoRepositorio;
    private final InstrucaoTrabalhoRepositorio instrucaoRepositorio;
    private final InstrucaoTrabalhoServico instrucaoServico;

    public EventoInstrucaoVmtServico(EventoInstrucaoVmtRepositorio eventoRepositorio,
                                     InstrucaoTrabalhoRepositorio instrucaoRepositorio,
                                     InstrucaoTrabalhoServico instrucaoServico) {
        this.eventoRepositorio = eventoRepositorio;
        this.instrucaoRepositorio = instrucaoRepositorio;
        this.instrucaoServico = instrucaoServico;
    }

    public EventoInstrucaoVmt processar(String eventId,
                                        Long instructionId,
                                        TipoEventoVmt tipoEvento,
                                        StatusInstrucao statusEsperado,
                                        LocalDateTime timestamp,
                                        String resultado,
                                        String payload) {
        String identificador = obrigatorio(eventId, "eventId");
        if (eventoRepositorio.existsByEventId(identificador)) {
            throw new IllegalStateException("Evento VMT duplicado: " + identificador);
        }
        if (tipoEvento == null || statusEsperado == null) {
            throw new IllegalArgumentException("Tipo do evento e estado esperado devem ser informados");
        }

        InstrucaoTrabalho instrucao = instrucaoRepositorio.findByIdForUpdate(instructionId)
                .orElseThrow(() -> new NoSuchElementException("Instrução de trabalho não encontrada"));
        if (instrucao.getStatus() != statusEsperado) {
            throw new IllegalStateException(String.format(
                    "Evento fora de sequência: esperado %s, estado persistido %s",
                    statusEsperado, instrucao.getStatus()));
        }

        LocalDateTime ocorridoEm = timestamp != null ? timestamp : LocalDateTime.now();
        aplicarTransicao(instrucao, tipoEvento, ocorridoEm, resultado);

        EventoInstrucaoVmt evento = new EventoInstrucaoVmt();
        evento.setEventId(identificador);
        evento.setInstrucao(instrucao);
        evento.setTipoEvento(tipoEvento);
        evento.setStatusEsperado(statusEsperado);
        evento.setOcorridoEm(ocorridoEm);
        evento.setResultado(normalizar(resultado));
        evento.setPayload(normalizar(payload));
        try {
            instrucaoRepositorio.saveAndFlush(instrucao);
            return eventoRepositorio.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Evento VMT duplicado ou inválido: " + identificador);
        }
    }

    @Transactional(readOnly = true)
    public List<EventoInstrucaoVmt> listar(Long instructionId) {
        if (!instrucaoRepositorio.existsById(instructionId)) {
            throw new NoSuchElementException("Instrução de trabalho não encontrada");
        }
        return eventoRepositorio.findByInstrucaoIdOrderByProcessadoEmAsc(instructionId);
    }

    private void aplicarTransicao(InstrucaoTrabalho instrucao,
                                  TipoEventoVmt tipoEvento,
                                  LocalDateTime timestamp,
                                  String resultado) {
        switch (tipoEvento) {
            case ACEITE:
                exigir(instrucao, StatusInstrucao.PENDENTE);
                instrucao.setStatus(StatusInstrucao.ACEITA);
                instrucao.setAceitaEm(timestamp);
                break;
            case INICIO:
                exigir(instrucao, StatusInstrucao.ACEITA);
                instrucao.setStatus(StatusInstrucao.EM_EXECUCAO);
                instrucao.setIniciadaEm(timestamp);
                break;
            case FALHA:
                if (instrucao.getStatus() != StatusInstrucao.ACEITA
                        && instrucao.getStatus() != StatusInstrucao.EM_EXECUCAO) {
                    throw new IllegalStateException("Falha VMT somente é permitida após aceite ou início");
                }
                instrucao.setStatus(StatusInstrucao.FALHA);
                instrucao.setFalhaEm(timestamp);
                instrucao.setResultadoVmt(obrigatorio(resultado, "Resultado da falha"));
                break;
            case CONCLUSAO:
                exigir(instrucao, StatusInstrucao.EM_EXECUCAO);
                instrucaoServico.concluir(instrucao.getId());
                instrucao.setConcluidaEm(timestamp);
                instrucao.setResultadoVmt(normalizar(resultado));
                break;
            default:
                throw new IllegalArgumentException("Tipo de evento VMT não suportado");
        }
    }

    private void exigir(InstrucaoTrabalho instrucao, StatusInstrucao esperado) {
        if (instrucao.getStatus() != esperado) {
            throw new IllegalStateException(String.format(
                    "Evento fora de sequência: necessário %s, estado persistido %s",
                    esperado, instrucao.getStatus()));
        }
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(campo + " deve ser informado");
        }
        return valor.trim();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
