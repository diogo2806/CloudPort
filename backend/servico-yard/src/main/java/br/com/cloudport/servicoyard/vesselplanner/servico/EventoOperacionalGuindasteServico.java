package br.com.cloudport.servicoyard.vesselplanner.servico;

import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.EncerrarParalisacaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.EventoOperacionalGuindasteResponse;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarHandoverRequest;
import br.com.cloudport.servicoyard.vesselplanner.dto.EventoOperacionalGuindasteDtos.RegistrarParalisacaoRequest;
import br.com.cloudport.servicoyard.vesselplanner.modelo.EventoOperacionalGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.ExecucaoSequenciaGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.NaturezaParalisacaoGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.modelo.TipoEventoOperacionalGuindaste;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.EventoOperacionalGuindasteRepositorio;
import br.com.cloudport.servicoyard.vesselplanner.repositorio.ExecucaoSequenciaGuindasteRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventoOperacionalGuindasteServico {

    private final ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio;
    private final EventoOperacionalGuindasteRepositorio eventoRepositorio;

    public EventoOperacionalGuindasteServico(
            ExecucaoSequenciaGuindasteRepositorio execucaoRepositorio,
            EventoOperacionalGuindasteRepositorio eventoRepositorio) {
        this.execucaoRepositorio = execucaoRepositorio;
        this.eventoRepositorio = eventoRepositorio;
    }

    @Transactional
    public EventoOperacionalGuindasteResponse registrarParalisacao(
            Long execucaoId,
            RegistrarParalisacaoRequest request,
            String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        validarGuindaste(execucao, request.guindasteId());

        LocalDateTime inicio = request.inicio() == null ? LocalDateTime.now() : request.inicio();
        validarPeriodo(request.natureza(), inicio, request.fim());
        List<EventoOperacionalGuindaste> existentes = buscarParalisacoes(
                execucaoId,
                request.guindasteId());
        validarSemSobreposicao(existentes, inicio, request.fim(), null);

        EventoOperacionalGuindaste evento = new EventoOperacionalGuindaste();
        evento.setExecucao(execucao);
        evento.setGuindasteId(request.guindasteId());
        evento.setTipo(TipoEventoOperacionalGuindaste.PARALISACAO);
        evento.setNatureza(request.natureza());
        evento.setInicio(inicio);
        evento.setFim(request.fim());
        evento.setMotivo(normalizarObrigatorio(request.motivo(), "motivo"));
        evento.setImpacto(normalizarObrigatorio(request.impacto(), "impacto"));
        evento.setTurnoOrigem(normalizarObrigatorio(request.turno(), "turno"));
        evento.setResponsavel(normalizarUsuario(usuario));
        evento.setPendencias(normalizarOpcional(request.pendencias()));
        evento.setObservacao(normalizarOpcional(request.observacao()));
        return mapear(eventoRepositorio.saveAndFlush(evento));
    }

    @Transactional
    public EventoOperacionalGuindasteResponse encerrarParalisacao(
            Long execucaoId,
            Long eventoId,
            EncerrarParalisacaoRequest request,
            String usuario) {
        buscarExecucao(execucaoId);
        EventoOperacionalGuindaste evento = eventoRepositorio.findLockedById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Paralisação de guindaste não encontrada: " + eventoId));
        if (!Objects.equals(evento.getExecucao().getId(), execucaoId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "A paralisação não pertence à execução informada.");
        }

        LocalDateTime fim = request.fim() == null ? LocalDateTime.now() : request.fim();
        List<EventoOperacionalGuindaste> existentes = buscarParalisacoes(
                execucaoId,
                evento.getGuindasteId());
        validarSemSobreposicao(existentes, evento.getInicio(), fim, evento.getId());
        executarTransicao(() -> evento.encerrar(
                fim,
                normalizarUsuario(usuario),
                normalizarOpcional(request.observacao())));
        return mapear(eventoRepositorio.saveAndFlush(evento));
    }

    @Transactional
    public EventoOperacionalGuindasteResponse registrarHandover(
            Long execucaoId,
            RegistrarHandoverRequest request,
            String usuario) {
        ExecucaoSequenciaGuindaste execucao = buscarExecucao(execucaoId);
        validarGuindaste(execucao, request.guindasteId());
        LocalDateTime instante = request.ocorridoEm() == null ? LocalDateTime.now() : request.ocorridoEm();

        EventoOperacionalGuindaste evento = new EventoOperacionalGuindaste();
        evento.setExecucao(execucao);
        evento.setGuindasteId(request.guindasteId());
        evento.setTipo(TipoEventoOperacionalGuindaste.HANDOVER);
        evento.setInicio(instante);
        evento.setFim(instante);
        evento.setTurnoOrigem(normalizarObrigatorio(request.turnoOrigem(), "turno de origem"));
        evento.setTurnoDestino(normalizarObrigatorio(request.turnoDestino(), "turno de destino"));
        evento.setResponsavel(normalizarUsuario(usuario));
        evento.setResponsavelDestino(normalizarObrigatorio(
                request.responsavelDestino(),
                "responsável do turno de destino"));
        evento.setPendencias(normalizarObrigatorio(request.pendencias(), "pendências operacionais"));
        evento.setObservacao(normalizarOpcional(request.observacao()));
        return mapear(eventoRepositorio.saveAndFlush(evento));
    }

    @Transactional
    public List<EventoOperacionalGuindasteResponse> listar(Long execucaoId) {
        buscarExecucao(execucaoId);
        return eventoRepositorio.findByExecucaoIdOrderByInicioDesc(execucaoId).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @Transactional
    public void validarGuindasteDisponivel(
            Long execucaoId,
            Integer guindasteId,
            LocalDateTime ocorridoEm) {
        LocalDateTime instante = ocorridoEm == null ? LocalDateTime.now() : ocorridoEm;
        EventoOperacionalGuindaste bloqueio = buscarParalisacoes(execucaoId, guindasteId).stream()
                .filter(evento -> !evento.getInicio().isAfter(instante))
                .filter(evento -> evento.getFim() == null || instante.isBefore(evento.getFim()))
                .findFirst()
                .orElse(null);
        if (bloqueio != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O guindaste " + guindasteId + " está paralisado desde "
                            + bloqueio.getInicio() + ". Motivo: " + bloqueio.getMotivo());
        }
    }

    private ExecucaoSequenciaGuindaste buscarExecucao(Long execucaoId) {
        return execucaoRepositorio.findById(execucaoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Execução de guindastes não encontrada: " + execucaoId));
    }

    private List<EventoOperacionalGuindaste> buscarParalisacoes(
            Long execucaoId,
            Integer guindasteId) {
        return eventoRepositorio.findByExecucaoIdAndGuindasteIdAndTipoOrderByInicioAsc(
                execucaoId,
                guindasteId,
                TipoEventoOperacionalGuindaste.PARALISACAO);
    }

    private void validarGuindaste(ExecucaoSequenciaGuindaste execucao, Integer guindasteId) {
        boolean existe = execucao.getMovimentos().stream()
                .anyMatch(movimento -> Objects.equals(movimento.getGuindasteId(), guindasteId));
        if (!existe) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O guindaste " + guindasteId + " não pertence à execução informada.");
        }
    }

    private void validarPeriodo(
            NaturezaParalisacaoGuindaste natureza,
            LocalDateTime inicio,
            LocalDateTime fim) {
        if (natureza == NaturezaParalisacaoGuindaste.PLANEJADA && fim == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A paralisação planejada deve informar início e fim.");
        }
        if (fim != null && !fim.isAfter(inicio)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O fim da paralisação deve ser posterior ao início.");
        }
    }

    private void validarSemSobreposicao(
            List<EventoOperacionalGuindaste> existentes,
            LocalDateTime inicio,
            LocalDateTime fim,
            Long eventoIgnorado) {
        LocalDateTime limiteNovo = fim == null ? LocalDateTime.MAX : fim;
        boolean sobreposta = existentes.stream()
                .filter(evento -> !Objects.equals(evento.getId(), eventoIgnorado))
                .anyMatch(evento -> {
                    LocalDateTime limiteExistente = evento.getFim() == null
                            ? LocalDateTime.MAX
                            : evento.getFim();
                    return inicio.isBefore(limiteExistente)
                            && evento.getInicio().isBefore(limiteNovo);
                });
        if (sobreposta) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A paralisação conflita com outro intervalo do mesmo guindaste.");
        }
    }

    private EventoOperacionalGuindasteResponse mapear(EventoOperacionalGuindaste evento) {
        return new EventoOperacionalGuindasteResponse(
                evento.getId(),
                evento.getExecucao().getId(),
                evento.getExecucao().getEstivagem().getId(),
                evento.getVersao(),
                evento.getGuindasteId(),
                evento.getTipo().name(),
                evento.getNatureza() == null ? null : evento.getNatureza().name(),
                evento.estado(),
                evento.getInicio(),
                evento.getFim(),
                evento.getMotivo(),
                evento.getImpacto(),
                evento.getTurnoOrigem(),
                evento.getTurnoDestino(),
                evento.getResponsavel(),
                evento.getResponsavelDestino(),
                evento.getPendencias(),
                evento.getObservacao(),
                evento.getEncerradoPor(),
                evento.getObservacaoEncerramento(),
                evento.getCriadoEm(),
                evento.getAtualizadoEm());
    }

    private String normalizarObrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe " + campo + ".");
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String normalizarUsuario(String usuario) {
        return StringUtils.hasText(usuario) ? usuario.trim() : "SISTEMA";
    }

    private void executarTransicao(Runnable transicao) {
        try {
            transicao.run();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }
}
