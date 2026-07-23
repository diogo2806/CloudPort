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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventoVmtWorkInstructionServico {

    private static final String VERSAO_CONTRATO_ATUAL = "2.0";

    private final EventoVmtWorkInstructionRepositorio eventoRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoWorkInstructionRepositorio historicoRepositorio;
    private final ConfirmacaoTransferenciaFisicaServico confirmacaoTransferenciaFisicaServico;

    public EventoVmtWorkInstructionServico(EventoVmtWorkInstructionRepositorio eventoRepositorio,
                                             OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                             HistoricoWorkInstructionRepositorio historicoRepositorio,
                                             ConfirmacaoTransferenciaFisicaServico confirmacaoTransferenciaFisicaServico) {
        this.eventoRepositorio = eventoRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
        this.confirmacaoTransferenciaFisicaServico = confirmacaoTransferenciaFisicaServico;
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

        validarContrato(request);
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
        validarEventoOperacional(instructionId, ordem, request);

        StatusConfirmacaoVmt estadoResultante = aplicarTransicao(instructionId, ordem, request);
        ordem.setStatusConfirmacaoVmt(estadoResultante);
        ordem.setUltimoEventoVmtId(eventId);
        ordem.setAtualizadoEm(LocalDateTime.now());
        OrdemTrabalhoPatio ordemSalva = ordemRepositorio.saveAndFlush(ordem);

        EventoVmtWorkInstruction evento = criarEvento(instructionId, eventId, estadoAtual,
                estadoResultante, request);

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

    private EventoVmtWorkInstruction criarEvento(Long instructionId,
                                                   String eventId,
                                                   StatusConfirmacaoVmt estadoAtual,
                                                   StatusConfirmacaoVmt estadoResultante,
                                                   EventoVmtWorkInstructionRequest request) {
        EventoVmtWorkInstruction evento = new EventoVmtWorkInstruction();
        evento.setEventId(eventId);
        evento.setOrdemTrabalhoPatioId(instructionId);
        evento.setTipoEvento(request.getTipoEvento());
        evento.setStatusEsperado(estadoAtual);
        evento.setStatusResultante(estadoResultante);
        evento.setOcorridoEm(request.getTimestamp());
        evento.setResultado(normalizar(request.getResultado(), 1000));
        evento.setPayload(normalizar(request.getPayload(), 10000));
        evento.setVersaoContrato(versaoContrato(request));
        evento.setTipoAcaoFisica(request.getTipoAcaoFisica());
        evento.setCodigoUnidadeLido(normalizar(request.getCodigoUnidadeLido(), 40));
        evento.setEquipamentoPatioId(request.getEquipamentoPatioId());
        evento.setEquipamentoIdentificador(normalizar(request.getEquipamentoIdentificador(), 80));
        evento.setOrigem(normalizar(request.getOrigem(), 120));
        evento.setDestino(normalizar(request.getDestino(), 120));
        evento.setLinhaOrigem(request.getLinhaOrigem());
        evento.setColunaOrigem(request.getColunaOrigem());
        evento.setCamadaOrigem(normalizar(request.getCamadaOrigem(), 40));
        evento.setLinhaDestino(request.getLinhaDestino());
        evento.setColunaDestino(request.getColunaDestino());
        evento.setCamadaDestino(normalizar(request.getCamadaDestino(), 40));
        evento.setSequenciaOperacional(request.getSequenciaOperacional());
        evento.setNumeroLacre(normalizar(request.getNumeroLacre(), 80));
        evento.setCodigoAvaria(normalizar(request.getCodigoAvaria(), 80));
        evento.setDescricaoAvaria(normalizar(request.getDescricaoAvaria(), 500));
        evento.setEvidenciaUrl(normalizar(request.getEvidenciaUrl(), 500));
        evento.setReeferConectadoDesejado(request.getReeferConectadoDesejado());
        evento.setTemperaturaReefer(request.getTemperaturaReefer());
        evento.setUnidadeAlvoRehandle(normalizar(request.getUnidadeAlvoRehandle(), 80));
        evento.setRehandleObrigatorio(request.getRehandleObrigatorio());
        evento.setSequenciaRehandle(request.getSequenciaRehandle());
        evento.setEtapaAnterior(normalizar(request.getEtapaAnterior(), 80));
        evento.setEtapaNova(normalizar(request.getEtapaNova(), 80));
        evento.setMotivoAjuste(normalizar(request.getMotivoAjuste(), 500));
        return evento;
    }

    private StatusConfirmacaoVmt aplicarTransicao(Long instructionId,
                                                    OrdemTrabalhoPatio ordem,
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
            case REGISTRO_LACRE:
            case REGISTRO_AVARIA:
            case REEFER_CONECTAR:
            case REEFER_DESCONECTAR:
            case REHANDLE_INICIO:
            case REHANDLE_CONCLUSAO:
            case AJUSTE_ETAPA:
                exigirEstado(request.getStatusEsperado(), StatusConfirmacaoVmt.EM_EXECUCAO, tipoEvento);
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
                validarRehandlesConcluidos(instructionId);
                confirmacaoTransferenciaFisicaServico.confirmar(ordem, request);
                ordem.setVmtConcluidoEm(request.getTimestamp());
                ordem.setResultadoVmt(normalizar(request.getResultado(), 1000));
                ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.CONCLUIDA);
                ordem.setConcluidoEm(request.getTimestamp());
                return StatusConfirmacaoVmt.CONCLUIDA;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de evento VMT nao suportado.");
        }
    }

    private void validarContrato(EventoVmtWorkInstructionRequest request) {
        String versao = versaoContrato(request);
        if (!VERSAO_CONTRATO_ATUAL.equals(versao) && !"1.0".equals(versao)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Versao de contrato VMT/RDT nao suportada: " + versao + ".");
        }
    }

    private String versaoContrato(EventoVmtWorkInstructionRequest request) {
        return StringUtils.hasText(request.getVersaoContrato())
                ? request.getVersaoContrato().trim()
                : VERSAO_CONTRATO_ATUAL;
    }

    private void validarEventoOperacional(Long instructionId,
                                           OrdemTrabalhoPatio ordem,
                                           EventoVmtWorkInstructionRequest request) {
        TipoEventoVmt tipo = request.getTipoEvento();
        if (tipo == null) return;
        switch (tipo) {
            case REGISTRO_LACRE:
                obrigatorio(request.getNumeroLacre(), "numeroLacre");
                exigirUnidade(ordem, request);
                break;
            case REGISTRO_AVARIA:
                obrigatorio(request.getCodigoAvaria(), "codigoAvaria");
                obrigatorio(request.getDescricaoAvaria(), "descricaoAvaria");
                obrigatorio(request.getEvidenciaUrl(), "evidenciaUrl");
                exigirUnidade(ordem, request);
                break;
            case REEFER_CONECTAR:
                exigirBooleano(request.getReeferConectadoDesejado(), true, tipo);
                exigirUnidade(ordem, request);
                break;
            case REEFER_DESCONECTAR:
                exigirBooleano(request.getReeferConectadoDesejado(), false, tipo);
                exigirUnidade(ordem, request);
                break;
            case REHANDLE_INICIO:
                obrigatorio(request.getUnidadeAlvoRehandle(), "unidadeAlvoRehandle");
                if (request.getSequenciaRehandle() == null || request.getSequenciaRehandle() < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "sequenciaRehandle deve ser maior que zero.");
                }
                break;
            case REHANDLE_CONCLUSAO:
                obrigatorio(request.getUnidadeAlvoRehandle(), "unidadeAlvoRehandle");
                validarRehandleAberto(instructionId, request);
                break;
            case AJUSTE_ETAPA:
                obrigatorio(request.getEtapaAnterior(), "etapaAnterior");
                obrigatorio(request.getEtapaNova(), "etapaNova");
                obrigatorio(request.getMotivoAjuste(), "motivoAjuste");
                if (request.getEtapaAnterior().trim().equalsIgnoreCase(request.getEtapaNova().trim())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "A nova etapa deve ser diferente da etapa anterior.");
                }
                break;
            default:
                break;
        }
    }

    private void exigirUnidade(OrdemTrabalhoPatio ordem, EventoVmtWorkInstructionRequest request) {
        String unidadeLida = obrigatorio(request.getCodigoUnidadeLido(), "codigoUnidadeLido");
        if (StringUtils.hasText(ordem.getCodigoConteiner())
                && !ordem.getCodigoConteiner().trim().equalsIgnoreCase(unidadeLida)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unidade lida diverge da work instruction.");
        }
    }

    private void exigirBooleano(Boolean valor, boolean esperado, TipoEventoVmt tipo) {
        if (valor == null || valor.booleanValue() != esperado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "reeferConectadoDesejado incompatível com o evento " + tipo + ".");
        }
    }

    private void validarRehandleAberto(Long instructionId, EventoVmtWorkInstructionRequest request) {
        boolean aberto = eventoRepositorio
                .findByOrdemTrabalhoPatioIdOrderByOcorridoEmAscProcessadoEmAsc(instructionId)
                .stream()
                .anyMatch(evento -> evento.getTipoEvento() == TipoEventoVmt.REHANDLE_INICIO
                        && igual(evento.getUnidadeAlvoRehandle(), request.getUnidadeAlvoRehandle())
                        && igual(evento.getSequenciaRehandle(), request.getSequenciaRehandle()));
        if (!aberto) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rehandle nao iniciado para a unidade e sequencia informadas.");
        }
    }

    private void validarRehandlesConcluidos(Long instructionId) {
        List<EventoVmtWorkInstruction> eventos = eventoRepositorio
                .findByOrdemTrabalhoPatioIdOrderByOcorridoEmAscProcessadoEmAsc(instructionId);
        Set<String> obrigatorios = new HashSet<>();
        Set<String> concluidos = new HashSet<>();
        for (EventoVmtWorkInstruction evento : eventos) {
            String chave = chaveRehandle(evento.getUnidadeAlvoRehandle(), evento.getSequenciaRehandle());
            if (evento.getTipoEvento() == TipoEventoVmt.REHANDLE_INICIO
                    && Boolean.TRUE.equals(evento.getRehandleObrigatorio())) {
                obrigatorios.add(chave);
            }
            if (evento.getTipoEvento() == TipoEventoVmt.REHANDLE_CONCLUSAO) {
                concluidos.add(chave);
            }
        }
        obrigatorios.removeAll(concluidos);
        if (!obrigatorios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existem rehandles obrigatorios pendentes: " + String.join(", ", obrigatorios) + ".");
        }
    }

    private String chaveRehandle(String unidade, Integer sequencia) {
        return valor(unidade, "SEM_UNIDADE") + "#" + (sequencia == null ? "SEM_SEQUENCIA" : sequencia);
    }

    private boolean igual(Object primeiro, Object segundo) {
        return primeiro == null ? segundo == null : primeiro.equals(segundo);
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
        if (atual != exigido) throw eventoForaDeSequencia(tipoEvento, atual);
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
        historico.setMotivo(evento.getTipoEvento() == TipoEventoVmt.CONCLUSAO
                ? "Transferencia fisica confirmada pelo operador."
                : "Comando operacional recebido do VMT/RDT.");
        historico.setDetalhes(normalizar(
                "eventId=" + evento.getEventId()
                        + "; versaoContrato=" + evento.getVersaoContrato()
                        + "; estadoEsperado=" + evento.getStatusEsperado()
                        + "; estadoResultante=" + evento.getStatusResultante()
                        + "; timestamp=" + evento.getOcorridoEm()
                        + "; resultado=" + valor(evento.getResultado(), "NAO_INFORMADO")
                        + "; unidadeLida=" + valor(evento.getCodigoUnidadeLido(), "NAO_INFORMADA")
                        + "; equipamento=" + valor(evento.getEquipamentoIdentificador(), "NAO_INFORMADO")
                        + "; lacre=" + valor(evento.getNumeroLacre(), "NAO_INFORMADO")
                        + "; avaria=" + valor(evento.getCodigoAvaria(), "NAO_INFORMADA")
                        + "; evidencia=" + valor(evento.getEvidenciaUrl(), "NAO_INFORMADA")
                        + "; reeferDesejado=" + evento.getReeferConectadoDesejado()
                        + "; temperaturaReefer=" + evento.getTemperaturaReefer()
                        + "; rehandle=" + valor(evento.getUnidadeAlvoRehandle(), "NAO_INFORMADO")
                        + "; rehandleObrigatorio=" + evento.getRehandleObrigatorio()
                        + "; sequenciaRehandle=" + evento.getSequenciaRehandle()
                        + "; etapaAnterior=" + valor(evento.getEtapaAnterior(), "NAO_INFORMADA")
                        + "; etapaNova=" + valor(evento.getEtapaNova(), "NAO_INFORMADA")
                        + "; motivoAjuste=" + valor(evento.getMotivoAjuste(), "NAO_INFORMADO")
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
        if (!StringUtils.hasText(valor)) return null;
        String normalizado = valor.trim();
        return normalizado.length() <= limite ? normalizado : normalizado.substring(0, limite);
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }
}
