package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRequest;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.EventoVmtWorkInstruction;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.EventoVmtWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventoVmtWorkInstructionServico {

    private final EventoVmtWorkInstructionRepositorio eventoRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;

    public EventoVmtWorkInstructionServico(EventoVmtWorkInstructionRepositorio eventoRepositorio,
                                            OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                            HistoricoWorkInstructionRepositorio historicoRepositorio) {
        this.eventoRepositorio = eventoRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
    }

    @Transactional
    public EventoVmtWorkInstructionRespostaDto processar(Long instructionId,
                                                          EventoVmtWorkInstructionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O evento VMT deve ser informado.");
        }
        String eventId = obrigatorio(request.getEventId(), "eventId");
        if (eventoRepositorio.findByEventId(eventId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Evento VMT duplicado: " + eventId + ".");
        }

        OrdemTrabalhoPatio ordem = ordemRepositorio.findOneById(instructionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Work instruction de patio nao encontrada."));
        validarEstadoOperacional(ordem);

        StatusConfirmacaoVmt estadoAtual = ordem.getStatusConfirmacaoVmt() == null
                ? StatusConfirmacaoVmt.PENDENTE
                : ordem.getStatusConfirmacaoVmt();
        if (request.getStatusEsperado() != estadoAtual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Evento VMT fora de sequencia: estado esperado " + request.getStatusEsperado()
                            + ", estado persistido " + estadoAtual + ".");
        }
        validarTimestamp(instructionId, request.getTimestamp());

        StatusConfirmacaoVmt estadoResultante = aplicarTransicao(ordem, request);
        ordem.setStatusConfirmacaoVmt(estadoResultante);
        ordem.setUltimoEventoVmtId(eventId);
        ordem.setAtualizadoEm(LocalDateTime.now());
        OrdemTrabalhoPatio ordemSalva = ordemRepositorio.saveAndFlush(ordem);

        EventoVmtWorkInstruction evento = new EventoVmtWorkInstruction();
        evento.setEventId(eventId);
        evento.setOrdemTrabalhoPatioId(instructionId);
        evento.setTipoEvento(request.getTipoEvento());
        evento.setStatusEsperado(estadoAtual);
        evento.setStatusResultante(estadoResultante);
        evento.setOcorridoEm(request.getTimestamp());
        evento.setResultado(normalizar(request.getResultado(), 1000));
        evento.setPayload(normalizar(request.getPayload(), 10000));

        try {
            EventoVmtWorkInstruction eventoSalvo = eventoRepositorio.saveAndFlush(evento);
            registrarHistorico(ordemSalva, eventoSalvo, request);
            return EventoVmtWorkInstructionRespostaDto.deEntidades(
                    eventoSalvo,
                    OrdemTrabalhoPatioRespostaDto.deEntidade(ordemSalva));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Evento VMT duplicado ou invalido: " + eventId + ".", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<EventoVmtWorkInstructionRespostaDto> listar(Long instructionId) {
        OrdemTrabalhoPatio ordem = ordemRepositorio.findById(instructionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Work instruction de patio nao encontrada."));
        OrdemTrabalhoPatioRespostaDto instrucao = OrdemTrabalhoPatioRespostaDto.deEntidade(ordem);
        return eventoRepositorio
                .findByOrdemTrabalhoPatioIdOrderByOcorridoEmAscProcessadoEmAsc(instructionId)
                .stream()
                .map(evento -> EventoVmtWorkInstructionRespostaDto.deEntidades(evento, instrucao))
                .toList();
    }

    private StatusConfirmacaoVmt aplicarTransicao(OrdemTrabalhoPatio ordem,
                                                    EventoVmtWorkInstructionRequest request) {
        TipoEventoVmt tipoEvento = request.getTipoEvento();
        if (tipoEvento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo do evento VMT deve ser informado.");
        }
        switch (tipoEvento) {
            case ACEITE:
                exigirEstado(request.getStatusEsperado(), StatusConfirmacaoVmt.PENDENTE, tipoEvento);
                ordem.setVmtAceitoEm(request.getTimestamp());
                return StatusConfirmacaoVmt.ACEITA;
            case INICIO:
                exigirEstado(request.getStatusEsperado(), StatusConfirmacaoVmt.ACEITA, tipoEvento);
                ordem.setVmtIniciadoEm(request.getTimestamp());
                return StatusConfirmacaoVmt.EM_EXECUCAO;
            case FALHA:
                if (request.getStatusEsperado() != StatusConfirmacaoVmt.ACEITA
                        && request.getStatusEsperado() != StatusConfirmacaoVmt.EM_EXECUCAO) {
                    throw eventoForaDeSequencia(tipoEvento, request.getStatusEsperado());
                }
                ordem.setVmtFalhaEm(request.getTimestamp());
                ordem.setResultadoVmt(obrigatorio(request.getResultado(), "resultado da falha"));
                ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.BLOQUEADA);
                ordem.setConcluidoEm(null);
                return StatusConfirmacaoVmt.FALHA;
            case CONCLUSAO:
                exigirEstado(request.getStatusEsperado(), StatusConfirmacaoVmt.EM_EXECUCAO, tipoEvento);
                ordem.setVmtConcluidoEm(request.getTimestamp());
                ordem.setResultadoVmt(normalizar(request.getResultado(), 1000));
                ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.CONCLUIDA);
                ordem.setConcluidoEm(request.getTimestamp());
                return StatusConfirmacaoVmt.CONCLUIDA;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de evento VMT nao suportado.");
        }
    }

    private void validarEstadoOperacional(OrdemTrabalhoPatio ordem) {
        if (ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.EM_EXECUCAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A work instruction deve estar despachada e em execucao para receber eventos VMT.");
        }
    }

    private void validarTimestamp(Long instructionId, LocalDateTime timestamp) {
        if (timestamp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O timestamp do evento VMT deve ser informado.");
        }
        eventoRepositorio
                .findFirstByOrdemTrabalhoPatioIdOrderByOcorridoEmDescProcessadoEmDesc(instructionId)
                .ifPresent(ultimo -> {
                    if (!timestamp.isAfter(ultimo.getOcorridoEm())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Evento VMT fora de sequencia temporal. O timestamp deve ser posterior ao ultimo evento.");
                    }
                });
    }

    private void exigirEstado(StatusConfirmacaoVmt atual,
                              StatusConfirmacaoVmt exigido,
                              TipoEventoVmt tipoEvento) {
        if (atual != exigido) {
            throw eventoForaDeSequencia(tipoEvento, atual);
        }
    }

    private ResponseStatusException eventoForaDeSequencia(TipoEventoVmt tipoEvento,
                                                            StatusConfirmacaoVmt estado) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Evento VMT " + tipoEvento + " fora de sequencia para o estado " + estado + ".");
    }

    private void registrarHistorico(OrdemTrabalhoPatio ordem,
                                     EventoVmtWorkInstruction evento,
                                     EventoVmtWorkInstructionRequest request) {
        HistoricoOperacaoPatio historico = new HistoricoOperacaoPatio();
        historico.setWorkQueueId(ordem.getWorkQueueId());
        historico.setOrdemTrabalhoPatioId(ordem.getId());
        historico.setAcao("WORK_INSTRUCTION_VMT_" + evento.getTipoEvento().name());
        historico.setUsuario(StringUtils.hasText(request.getOperador())
                ? request.getOperador().trim()
                : "integracao-vmt");
        historico.setMotivo("Confirmacao recebida do VMT.");
        historico.setDetalhes(normalizar(
                "eventId=" + evento.getEventId()
                        + "; estadoEsperado=" + evento.getStatusEsperado()
                        + "; estadoResultante=" + evento.getStatusResultante()
                        + "; timestamp=" + evento.getOcorridoEm()
                        + "; resultado=" + valor(evento.getResultado(), "NAO_INFORMADO")
                        + "; correlationId=" + valor(request.getCorrelationId(), "NAO_INFORMADO"),
                2000));
        historico.setCriadoEm(LocalDateTime.now());
        historicoRepositorio.save(historico);
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " deve ser informado.");
        }
        return valor.trim();
    }

    private String normalizar(String valor, int limite) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.length() <= limite ? normalizado : normalizado.substring(0, limite);
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }
}
