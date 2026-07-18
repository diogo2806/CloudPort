package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.AuditoriaSequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.EventoOutboxSequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.SequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusSequenciaGuindaste;
import br.com.cloudport.serviconaviosiderurgico.dto.ComandosSequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.dto.SequenciaGuindasteDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.AuditoriaSequenciaGuindasteRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.EventoOutboxSequenciaGuindasteRepositorio;
import br.com.cloudport.serviconaviosiderurgico.repositorio.SequenciaGuindasteRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SequenciaGuindasteServico {

    private static final String TIPO_TRANSICAO = "TRANSITION";
    private static final String TIPO_RECONCILIACAO = "RECONCILIATION_ALERT";

    private final SequenciaGuindasteRepositorio sequenciaRepositorio;
    private final AuditoriaSequenciaGuindasteRepositorio auditoriaRepositorio;
    private final EventoOutboxSequenciaGuindasteRepositorio outboxRepositorio;

    public SequenciaGuindasteServico(
            SequenciaGuindasteRepositorio sequenciaRepositorio,
            AuditoriaSequenciaGuindasteRepositorio auditoriaRepositorio,
            EventoOutboxSequenciaGuindasteRepositorio outboxRepositorio
    ) {
        this.sequenciaRepositorio = sequenciaRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.outboxRepositorio = outboxRepositorio;
    }

    @Transactional
    public SequenciaGuindasteDTO criarOuObter(ComandosSequenciaGuindasteDTO.Criar comando) {
        String movementId = obrigatorio(comando.movementId(), "movementId e obrigatorio.");
        return sequenciaRepositorio.findByMovementId(movementId)
                .map(this::paraDTO)
                .orElseGet(() -> criar(comando, movementId));
    }

    @Transactional
    public SequenciaGuindasteDTO iniciar(String movementId, ComandosSequenciaGuindasteDTO.Transicao comando) {
        SequenciaGuindaste sequencia = buscarEntidade(movementId);
        if (sequencia.getStatus() == StatusSequenciaGuindaste.STARTED) {
            return paraDTO(sequencia);
        }
        validarEstado(sequencia, List.of(StatusSequenciaGuindaste.PLANNED, StatusSequenciaGuindaste.PAUSED), "iniciar");
        StatusSequenciaGuindaste anterior = sequencia.getStatus();
        sequencia.setStatus(StatusSequenciaGuindaste.STARTED);
        if (sequencia.getStartedAt() == null) {
            sequencia.setStartedAt(LocalDateTime.now());
        }
        sequencia.setOperatorId(operador(comando));
        SequenciaGuindaste salva = salvarComConcorrencia(sequencia);
        auditar(salva, anterior, salva.getStatus(), operador(comando), comando.reason());
        registrarOutbox(salva);
        return paraDTO(salva);
    }

    @Transactional
    public SequenciaGuindasteDTO pausar(String movementId, ComandosSequenciaGuindasteDTO.Transicao comando) {
        SequenciaGuindaste sequencia = buscarEntidade(movementId);
        if (sequencia.getStatus() == StatusSequenciaGuindaste.PAUSED) {
            return paraDTO(sequencia);
        }
        validarEstado(sequencia, List.of(StatusSequenciaGuindaste.STARTED), "pausar");
        String motivo = obrigatorio(comando.reason(), "Motivo da pausa e obrigatorio.");
        StatusSequenciaGuindaste anterior = sequencia.getStatus();
        sequencia.setStatus(StatusSequenciaGuindaste.PAUSED);
        sequencia.setOperatorId(operador(comando));
        sequencia.setNotes(anexarNota(sequencia.getNotes(), "Pausa", motivo));
        SequenciaGuindaste salva = salvarComConcorrencia(sequencia);
        auditar(salva, anterior, salva.getStatus(), operador(comando), motivo);
        return paraDTO(salva);
    }

    @Transactional
    public SequenciaGuindasteDTO finalizar(String movementId, ComandosSequenciaGuindasteDTO.Transicao comando) {
        SequenciaGuindaste sequencia = buscarEntidade(movementId);
        if (sequencia.getStatus() == StatusSequenciaGuindaste.FINISHED) {
            return paraDTO(sequencia);
        }
        validarEstado(sequencia, List.of(StatusSequenciaGuindaste.STARTED), "finalizar");
        StatusSequenciaGuindaste anterior = sequencia.getStatus();
        sequencia.setStatus(StatusSequenciaGuindaste.FINISHED);
        sequencia.setFinishedAt(LocalDateTime.now());
        sequencia.setOperatorId(operador(comando));
        SequenciaGuindaste salva = salvarComConcorrencia(sequencia);
        auditar(salva, anterior, salva.getStatus(), operador(comando), comando.reason());
        registrarOutbox(salva);
        return paraDTO(salva);
    }

    @Transactional
    public SequenciaGuindasteDTO cancelar(String movementId, ComandosSequenciaGuindasteDTO.Transicao comando) {
        SequenciaGuindaste sequencia = buscarEntidade(movementId);
        if (sequencia.getStatus() == StatusSequenciaGuindaste.CANCELLED) {
            return paraDTO(sequencia);
        }
        if (sequencia.getStatus() == StatusSequenciaGuindaste.FINISHED) {
            throw conflito(sequencia, "cancelar");
        }
        String motivo = obrigatorio(comando.reason(), "Motivo do cancelamento e obrigatorio.");
        StatusSequenciaGuindaste anterior = sequencia.getStatus();
        sequencia.setStatus(StatusSequenciaGuindaste.CANCELLED);
        sequencia.setOperatorId(operador(comando));
        sequencia.setNotes(anexarNota(sequencia.getNotes(), "Cancelamento", motivo));
        SequenciaGuindaste salva = salvarComConcorrencia(sequencia);
        auditar(salva, anterior, salva.getStatus(), operador(comando), motivo);
        return paraDTO(salva);
    }

    @Transactional(readOnly = true)
    public SequenciaGuindasteDTO buscar(String movementId) {
        return paraDTO(buscarEntidade(movementId));
    }

    @Transactional(readOnly = true)
    public List<SequenciaGuindasteDTO> listar(
            String vesselVisitId,
            StatusSequenciaGuindaste status,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Specification<SequenciaGuindaste> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(vesselVisitId)) {
                predicates.add(criteriaBuilder.equal(root.get("vesselVisitId"), vesselVisitId.trim()));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("plannedStart"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("plannedStart"), to));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return sequenciaRepositorio.findAll(specification, Sort.by(Sort.Direction.ASC, "plannedStart"))
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SequenciaGuindasteDTO.Auditoria> listarAuditoria(String movementId) {
        buscarEntidade(movementId);
        return auditoriaRepositorio.findByMovementIdOrderByOccurredAtDesc(normalizarId(movementId))
                .stream()
                .map(this::paraAuditoriaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SequenciaGuindaste> listarParaReconciliacao() {
        return sequenciaRepositorio.findByStatusIn(List.of(
                StatusSequenciaGuindaste.STARTED,
                StatusSequenciaGuindaste.FINISHED));
    }

    @Transactional
    public void registrarAlertaReconciliacao(String movementId, String mensagem) {
        String id = normalizarId(movementId);
        if (auditoriaRepositorio.existsByMovementIdAndTypeAndReason(id, TIPO_RECONCILIACAO, mensagem)) {
            return;
        }
        SequenciaGuindaste sequencia = buscarEntidade(id);
        AuditoriaSequenciaGuindaste auditoria = new AuditoriaSequenciaGuindaste();
        auditoria.setMovementId(id);
        auditoria.setType(TIPO_RECONCILIACAO);
        auditoria.setStatusBefore(sequencia.getStatus());
        auditoria.setStatusAfter(sequencia.getStatus());
        auditoria.setOperatorId("reconciliation-job");
        auditoria.setReason(mensagem);
        auditoriaRepositorio.save(auditoria);
    }

    private SequenciaGuindasteDTO criar(ComandosSequenciaGuindasteDTO.Criar comando, String movementId) {
        SequenciaGuindaste sequencia = new SequenciaGuindaste();
        sequencia.setMovementId(movementId);
        sequencia.setVesselVisitId(obrigatorio(comando.vesselVisitId(), "vesselVisitId e obrigatorio."));
        sequencia.setCraneId(obrigatorio(comando.craneId(), "craneId e obrigatorio."));
        sequencia.setLoadUnitId(obrigatorio(comando.loadUnitId(), "loadUnitId e obrigatorio."));
        sequencia.setPlannedStart(comando.plannedStart());
        sequencia.setNotes(limpar(comando.notes()));
        sequencia.setStatus(StatusSequenciaGuindaste.PLANNED);
        try {
            SequenciaGuindaste salva = sequenciaRepositorio.saveAndFlush(sequencia);
            auditar(salva, null, StatusSequenciaGuindaste.PLANNED, "sistema", "Sequencia criada.");
            return paraDTO(salva);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe uma sequencia para movementId " + movementId + ".", ex);
        }
    }

    private SequenciaGuindaste buscarEntidade(String movementId) {
        String id = normalizarId(movementId);
        return sequenciaRepositorio.findByMovementId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sequencia de guindaste nao encontrada para movementId " + id + "."));
    }

    private SequenciaGuindaste salvarComConcorrencia(SequenciaGuindaste sequencia) {
        try {
            return sequenciaRepositorio.saveAndFlush(sequencia);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A sequencia foi alterada por outro operador. Recarregue os dados e tente novamente.",
                    ex);
        }
    }

    private void validarEstado(
            SequenciaGuindaste sequencia,
            List<StatusSequenciaGuindaste> permitidos,
            String acao
    ) {
        if (!permitidos.contains(sequencia.getStatus())) {
            throw conflito(sequencia, acao);
        }
    }

    private ResponseStatusException conflito(SequenciaGuindaste sequencia, String acao) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Nao e permitido " + acao + " a sequencia " + sequencia.getMovementId()
                        + " no estado " + sequencia.getStatus() + ".");
    }

    private void auditar(
            SequenciaGuindaste sequencia,
            StatusSequenciaGuindaste anterior,
            StatusSequenciaGuindaste atual,
            String operador,
            String motivo
    ) {
        AuditoriaSequenciaGuindaste auditoria = new AuditoriaSequenciaGuindaste();
        auditoria.setMovementId(sequencia.getMovementId());
        auditoria.setType(TIPO_TRANSICAO);
        auditoria.setStatusBefore(anterior);
        auditoria.setStatusAfter(atual);
        auditoria.setOperatorId(operador);
        auditoria.setReason(limpar(motivo));
        auditoriaRepositorio.save(auditoria);
    }

    private void registrarOutbox(SequenciaGuindaste sequencia) {
        String eventKey = sequencia.getMovementId() + ":" + sequencia.getStatus().name();
        if (outboxRepositorio.existsByEventKey(eventKey)) {
            return;
        }
        EventoOutboxSequenciaGuindaste evento = new EventoOutboxSequenciaGuindaste();
        evento.setEventKey(eventKey);
        evento.setMovementId(sequencia.getMovementId());
        evento.setEventType("CRANE_SEQUENCE_" + sequencia.getStatus().name());
        evento.setPayload("{\"movementId\":\"" + escapar(sequencia.getMovementId())
                + "\",\"vesselVisitId\":\"" + escapar(sequencia.getVesselVisitId())
                + "\",\"loadUnitId\":\"" + escapar(sequencia.getLoadUnitId())
                + "\",\"status\":\"" + sequencia.getStatus().name() + "\"}");
        try {
            outboxRepositorio.saveAndFlush(evento);
        } catch (DataIntegrityViolationException ex) {
            // Outro fluxo concorrente ja registrou o mesmo evento idempotente.
        }
    }

    private SequenciaGuindasteDTO paraDTO(SequenciaGuindaste sequencia) {
        return new SequenciaGuindasteDTO(
                sequencia.getMovementId(),
                sequencia.getVesselVisitId(),
                sequencia.getCraneId(),
                sequencia.getLoadUnitId(),
                sequencia.getPlannedStart(),
                sequencia.getStartedAt(),
                sequencia.getFinishedAt(),
                sequencia.getStatus(),
                sequencia.getOperatorId(),
                sequencia.getNotes(),
                sequencia.getCreatedAt(),
                sequencia.getUpdatedAt(),
                sequencia.getVersion());
    }

    private SequenciaGuindasteDTO.Auditoria paraAuditoriaDTO(AuditoriaSequenciaGuindaste auditoria) {
        return new SequenciaGuindasteDTO.Auditoria(
                auditoria.getType(),
                auditoria.getStatusBefore(),
                auditoria.getStatusAfter(),
                auditoria.getOperatorId(),
                auditoria.getReason(),
                auditoria.getOccurredAt());
    }

    private String anexarNota(String atual, String tipo, String motivo) {
        String nova = "[" + LocalDateTime.now() + "] " + tipo + ": " + motivo;
        return StringUtils.hasText(atual) ? atual.trim() + System.lineSeparator() + nova : nova;
    }

    private String operador(ComandosSequenciaGuindasteDTO.Transicao comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Comando de transicao e obrigatorio.");
        }
        return obrigatorio(comando.operatorId(), "operatorId e obrigatorio.");
    }

    private String normalizarId(String valor) {
        return obrigatorio(valor, "movementId e obrigatorio.");
    }

    private String obrigatorio(String valor, String mensagem) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
