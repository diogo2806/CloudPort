package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.StatusProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.CargaGeralTipos.TipoEventoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.ItemOperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.OperacaoStuffUnstuff;
import br.com.cloudport.servicocargageral.dominio.ProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.ReservaLoteProgramacaoDocaCarga;
import br.com.cloudport.servicocargageral.dominio.PlanoStuffUnstuffVersao.StatusPlano;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.CancelarProgramacaoDocaRequest;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ProgramacaoDocaResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ReservaLoteProgramacaoResposta;
import br.com.cloudport.servicocargageral.dto.StuffUnstuffDTOs.ReservarProgramacaoDocaRequest;
import br.com.cloudport.servicocargageral.repositorio.OperacaoStuffUnstuffRepositorio;
import br.com.cloudport.servicocargageral.repositorio.PlanoStuffUnstuffVersaoRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ProgramacaoDocaCargaRepositorio;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgramacaoDocaCargaServico {

    private static final List<StatusProgramacaoDocaCarga> STATUS_ATIVOS = List.of(
            StatusProgramacaoDocaCarga.RESERVADA,
            StatusProgramacaoDocaCarga.EM_USO);

    private final ProgramacaoDocaCargaRepositorio programacaoRepositorio;
    private final OperacaoStuffUnstuffRepositorio operacaoRepositorio;
    private final PlanoStuffUnstuffVersaoRepositorio planoRepositorio;

    public ProgramacaoDocaCargaServico(
            ProgramacaoDocaCargaRepositorio programacaoRepositorio,
            OperacaoStuffUnstuffRepositorio operacaoRepositorio,
            PlanoStuffUnstuffVersaoRepositorio planoRepositorio) {
        this.programacaoRepositorio = programacaoRepositorio;
        this.operacaoRepositorio = operacaoRepositorio;
        this.planoRepositorio = planoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ProgramacaoDocaResposta> listar(OffsetDateTime inicio, OffsetDateTime fim) {
        if ((inicio == null) != (fim == null)) {
            throw requisicaoInvalida("Informe início e fim para filtrar a agenda operacional.");
        }
        List<ProgramacaoDocaCarga> programacoes;
        if (inicio == null) {
            programacoes = programacaoRepositorio.findAllByOrderByJanelaInicioDesc();
        } else {
            if (!fim.isAfter(inicio)) {
                throw requisicaoInvalida("O fim do filtro deve ser posterior ao início.");
            }
            programacoes = programacaoRepositorio
                    .findByJanelaInicioLessThanAndJanelaFimGreaterThanOrderByJanelaInicioAsc(fim, inicio);
        }
        return programacoes.stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramacaoDocaResposta obter(UUID operacaoId) {
        return mapear(programacaoRepositorio.findByOperacao_Id(operacaoId)
                .orElseThrow(() -> naoEncontrada("Programação de doca não encontrada para a operação.")));
    }

    @Transactional
    public ProgramacaoDocaResposta reservar(UUID operacaoId, ReservarProgramacaoDocaRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(operacaoId);
        validarOperacaoPlanejavel(operacao);
        if (!planoRepositorio.existsByOperacao_IdAndStatus(operacaoId, StatusPlano.LIBERADO)) {
            throw conflito("A programação de doca exige uma versão liberada do plano.");
        }

        ProgramacaoDocaCarga programacao = programacaoRepositorio.findComBloqueioByOperacao_Id(operacaoId)
                .orElseGet(ProgramacaoDocaCarga::new);
        validarConflitos(programacao, operacao, request);
        executarComEstadoValido(() -> programacao.reservar(
                operacao,
                request.docaId(),
                request.areaEsperaId(),
                request.recursoId(),
                request.janelaInicio(),
                request.janelaFim(),
                request.usuario(),
                request.observacao()));

        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PROGRAMACAO_DOCA_RESERVADA,
                request.usuario(),
                request.correlationId(),
                "Doca " + normalizar(request.docaId())
                        + ", área de espera " + normalizar(request.areaEsperaId())
                        + " e recurso " + normalizar(request.recursoId())
                        + " reservados de " + request.janelaInicio() + " até " + request.janelaFim() + ".");
        operacaoRepositorio.save(operacao);
        try {
            return mapear(programacaoRepositorio.saveAndFlush(programacao));
        } catch (DataIntegrityViolationException exception) {
            throw conflito("A doca, a área de espera, o recurso, o contêiner ou um cargo lot já está reservado em parte da janela informada.");
        }
    }

    @Transactional
    public ProgramacaoDocaResposta cancelar(UUID operacaoId, CancelarProgramacaoDocaRequest request) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(operacaoId);
        validarOperacaoPlanejavel(operacao);
        ProgramacaoDocaCarga programacao = buscarProgramacaoComBloqueio(operacaoId);
        executarComEstadoValido(() -> programacao.cancelar(request.usuario(), request.motivo(), false));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PROGRAMACAO_DOCA_CANCELADA,
                request.usuario(),
                request.correlationId(),
                "Programação de doca cancelada: " + request.motivo());
        operacaoRepositorio.save(operacao);
        return mapear(programacaoRepositorio.save(programacao));
    }

    @Transactional
    public void iniciarParaOperacao(UUID operacaoId, String usuario, String correlationId) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(operacaoId);
        ProgramacaoDocaCarga programacao = buscarProgramacaoComBloqueio(operacaoId);
        executarComEstadoValido(() -> programacao.iniciar(usuario));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.STAGING_INICIADO,
                usuario,
                correlationId,
                "Ocupação iniciada na doca " + programacao.getDocaId()
                        + ", área " + programacao.getAreaEsperaId()
                        + " e recurso " + programacao.getRecursoId() + ".");
        operacaoRepositorio.save(operacao);
        programacaoRepositorio.save(programacao);
    }

    @Transactional(readOnly = true)
    public void exigirEmUso(UUID operacaoId) {
        ProgramacaoDocaCarga programacao = programacaoRepositorio.findByOperacao_Id(operacaoId)
                .orElseThrow(() -> conflito("A execução física exige programação de doca reservada e iniciada."));
        if (programacao.getStatus() != StatusProgramacaoDocaCarga.EM_USO) {
            throw conflito("A execução física exige programação de doca em uso.");
        }
    }

    @Transactional
    public void concluirParaOperacao(UUID operacaoId, String usuario, String correlationId) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(operacaoId);
        ProgramacaoDocaCarga programacao = buscarProgramacaoComBloqueio(operacaoId);
        executarComEstadoValido(() -> programacao.concluir(usuario));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.STAGING_CONCLUIDO,
                usuario,
                correlationId,
                "Doca, área de espera, recurso, contêiner e cargo lots liberados da agenda operacional.");
        operacaoRepositorio.save(operacao);
        programacaoRepositorio.save(programacao);
    }

    @Transactional
    public void cancelarParaOperacao(UUID operacaoId, String usuario, String correlationId, String motivo) {
        OperacaoStuffUnstuff operacao = buscarOperacaoComBloqueio(operacaoId);
        ProgramacaoDocaCarga programacao = programacaoRepositorio.findComBloqueioByOperacao_Id(operacaoId)
                .orElse(null);
        if (programacao == null || programacao.getStatus() == StatusProgramacaoDocaCarga.CANCELADA) {
            return;
        }
        executarComEstadoValido(() -> programacao.cancelar(usuario, motivo, true));
        operacao.registrarEvento(
                TipoEventoStuffUnstuff.PROGRAMACAO_DOCA_CANCELADA,
                usuario,
                correlationId,
                "Recursos da programação liberados pelo cancelamento da operação: " + motivo);
        operacaoRepositorio.save(operacao);
        programacaoRepositorio.save(programacao);
    }

    @Transactional(readOnly = true)
    public boolean possuiReservaAtiva(UUID operacaoId) {
        return programacaoRepositorio.existsByOperacao_IdAndStatusIn(operacaoId, STATUS_ATIVOS);
    }

    private void validarConflitos(
            ProgramacaoDocaCarga atual,
            OperacaoStuffUnstuff operacao,
            ReservarProgramacaoDocaRequest request) {
        if (request.janelaInicio() == null || request.janelaFim() == null
                || !request.janelaFim().isAfter(request.janelaInicio())) {
            throw conflito("A janela operacional informada é inválida.");
        }
        Set<UUID> lotesOperacao = operacao.getItens().stream()
                .map(ItemOperacaoStuffUnstuff::getLote)
                .map(lote -> lote.getId())
                .collect(Collectors.toCollection(HashSet::new));
        List<ProgramacaoDocaCarga> candidatas = programacaoRepositorio
                .findByStatusInAndJanelaInicioLessThanAndJanelaFimGreaterThan(
                        STATUS_ATIVOS,
                        request.janelaFim(),
                        request.janelaInicio());
        for (ProgramacaoDocaCarga candidata : candidatas) {
            if (atual.getId() != null && atual.getId().equals(candidata.getId())) {
                continue;
            }
            if (normalizar(request.docaId()).equals(candidata.getDocaId())) {
                throw conflito("A doca " + candidata.getDocaId() + " já está reservada na janela informada.");
            }
            if (normalizar(request.areaEsperaId()).equals(candidata.getAreaEsperaId())) {
                throw conflito("A área de espera " + candidata.getAreaEsperaId() + " já está reservada na janela informada.");
            }
            if (normalizar(request.recursoId()).equals(candidata.getRecursoId())) {
                throw conflito("O recurso " + candidata.getRecursoId() + " já está reservado na janela informada.");
            }
            if (normalizar(operacao.getConteinerId()).equals(candidata.getConteinerId())) {
                throw conflito("O contêiner " + candidata.getConteinerId() + " já possui programação ativa na janela informada.");
            }
            boolean loteConflitante = candidata.getReservasLote().stream()
                    .map(ReservaLoteProgramacaoDocaCarga::getLoteId)
                    .anyMatch(lotesOperacao::contains);
            if (loteConflitante) {
                throw conflito("Um ou mais cargo lots já possuem programação ativa na janela informada.");
            }
        }
    }

    private void validarOperacaoPlanejavel(OperacaoStuffUnstuff operacao) {
        if (operacao.getStatus() != StatusOperacaoStuffUnstuff.PLANEJADA || operacao.possuiExecucao()) {
            throw conflito("A programação de doca só pode ser alterada antes da execução física.");
        }
    }

    private OperacaoStuffUnstuff buscarOperacaoComBloqueio(UUID id) {
        return operacaoRepositorio.findComBloqueioById(id)
                .orElseThrow(() -> naoEncontrada("Operação de stuff/unstuff não encontrada."));
    }

    private ProgramacaoDocaCarga buscarProgramacaoComBloqueio(UUID operacaoId) {
        return programacaoRepositorio.findComBloqueioByOperacao_Id(operacaoId)
                .orElseThrow(() -> conflito("A operação exige programação de doca antes do início."));
    }

    private ProgramacaoDocaResposta mapear(ProgramacaoDocaCarga programacao) {
        List<ReservaLoteProgramacaoResposta> lotes = programacao.getReservasLote().stream()
                .map(this::mapearLote)
                .collect(Collectors.toList());
        return new ProgramacaoDocaResposta(
                programacao.getId(),
                programacao.getOperacao().getId(),
                programacao.getOperacao().getTipo(),
                programacao.getConteinerId(),
                programacao.getDocaId(),
                programacao.getAreaEsperaId(),
                programacao.getRecursoId(),
                programacao.getJanelaInicio(),
                programacao.getJanelaFim(),
                programacao.getStatus(),
                programacao.getReservadoPor(),
                programacao.getReservadoEm(),
                programacao.getObservacaoReserva(),
                programacao.getIniciadoPor(),
                programacao.getIniciadoEm(),
                programacao.getConcluidoPor(),
                programacao.getConcluidoEm(),
                programacao.getCanceladoPor(),
                programacao.getCanceladoEm(),
                programacao.getMotivoCancelamento(),
                programacao.ocupaRecursos(),
                lotes);
    }

    private ReservaLoteProgramacaoResposta mapearLote(ReservaLoteProgramacaoDocaCarga reserva) {
        return new ReservaLoteProgramacaoResposta(
                reserva.getLoteId(),
                reserva.getLoteCodigo(),
                reserva.getQuantidadeReservada(),
                reserva.getVolumeReservadoM3(),
                reserva.getPesoReservadoKg());
    }

    private void executarComEstadoValido(Runnable acao) {
        try {
            acao.run();
        } catch (IllegalStateException exception) {
            throw conflito(exception.getMessage());
        }
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException requisicaoInvalida(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
