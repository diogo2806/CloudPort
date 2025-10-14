package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.config.GateFlowProperties;
import br.com.cloudport.servicogate.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.dto.GateFlowRequest;
import br.com.cloudport.servicogate.dto.ManualReleaseAction;
import br.com.cloudport.servicogate.dto.ManualReleaseRequest;
import br.com.cloudport.servicogate.dto.TosContainerStatus;
import br.com.cloudport.servicogate.dto.TosSyncResponse;
import br.com.cloudport.servicogate.dto.mapper.GateMapper;
import br.com.cloudport.servicogate.dto.mapper.GateOperadorMapper;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.MotivoExcecao;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import br.com.cloudport.servicogate.repository.GateEventRepository;
import br.com.cloudport.servicogate.repository.GatePassRepository;
import br.com.cloudport.servicogate.service.GateOperadorRealtimeService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GateFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateFlowService.class);

    private static final EnumSet<StatusAgendamento> STATUS_VALIDOS_ENTRADA = EnumSet.of(
            StatusAgendamento.CONFIRMADO,
            StatusAgendamento.EM_ATENDIMENTO,
            StatusAgendamento.EM_EXECUCAO
    );

    private static final EnumSet<StatusAgendamento> STATUS_VALIDOS_SAIDA = EnumSet.of(
            StatusAgendamento.EM_EXECUCAO,
            StatusAgendamento.EM_ATENDIMENTO
    );

    private final AgendamentoRepository agendamentoRepository;
    private final GatePassRepository gatePassRepository;
    private final GateEventRepository gateEventRepository;
    private final GateFlowProperties flowProperties;
    private final TosIntegrationService tosIntegrationService;
    private final GateMetrics gateMetrics;
    private final AgendamentoRealtimeService agendamentoRealtimeService;
    private final GateOperadorRealtimeService gateOperadorRealtimeService;

    public GateFlowService(AgendamentoRepository agendamentoRepository,
                           GatePassRepository gatePassRepository,
                           GateEventRepository gateEventRepository,
                           GateFlowProperties flowProperties,
                           TosIntegrationService tosIntegrationService,
                              GateMetrics gateMetrics,
                              AgendamentoRealtimeService agendamentoRealtimeService,
                              GateOperadorRealtimeService gateOperadorRealtimeService) {
        this.agendamentoRepository = agendamentoRepository;
        this.gatePassRepository = gatePassRepository;
        this.gateEventRepository = gateEventRepository;
        this.flowProperties = flowProperties;
        this.tosIntegrationService = tosIntegrationService;
        this.gateMetrics = gateMetrics;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
        this.gateOperadorRealtimeService = gateOperadorRealtimeService;
    }

    public GateDecisionDTO registrarEntrada(GateFlowRequest request) {
        LocalDateTime timestamp = resolverTimestamp(request.getTimestamp());
        Agendamento agendamento = localizarAgendamento(request);
        GatePass gatePass = obterOuCriarGatePass(agendamento);
        long inicioValidacao = System.nanoTime();
        boolean sucesso = false;
        try {
            TosContainerStatus statusContainer = tosIntegrationService.validarParaEntrada(agendamento);
            validarStatusParaEntrada(agendamento);
            validarDocumentos(agendamento);
            validarJanela(agendamento.getHorarioPrevistoChegada(), timestamp,
                    flowProperties.getToleranciaEntradaAntecipada(),
                    flowProperties.getToleranciaEntradaAtraso(),
                    "Horário de chegada fora da tolerância permitida");

            agendamento.setHorarioRealChegada(timestamp);
            agendamento.setStatus(StatusAgendamento.EM_EXECUCAO);
            gatePass.setDataEntrada(timestamp);
            gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);

            gatePassRepository.save(gatePass);
            agendamentoRepository.save(agendamento);

            GateEvent evento = registrarEvento(gatePass, StatusGate.LIBERADO, null,
                    "Entrada autorizada", resolverOperador(request.getOperador()), timestamp);
            LOGGER.info("Entrada autorizada para agendamento {} containerStatus={} customsLiberado={}",
                    agendamento.getCodigo(),
                    statusContainer != null ? statusContainer.getStatus() : null,
                    statusContainer != null && statusContainer.isLiberacaoAduaneira());

            sucesso = true;
            agendamentoRealtimeService.notificarStatus(agendamento);
            return GateDecisionDTO.autorizado(evento.getStatus(), agendamento, gatePass,
                    "Entrada liberada com sucesso");
        } catch (RuntimeException ex) {
            registrarEvento(gatePass, StatusGate.RETIDO, null, ex.getMessage(),
                    resolverOperador(request.getOperador()), timestamp);
            throw ex;
        } finally {
            Duration duracao = Duration.ofNanos(System.nanoTime() - inicioValidacao);
            gateMetrics.registrarTempoValidacao(duracao, sucesso);
        }
    }

    public GateDecisionDTO registrarSaida(GateFlowRequest request) {
        LocalDateTime timestamp = resolverTimestamp(request.getTimestamp());
        Agendamento agendamento = localizarAgendamento(request);
        GatePass gatePass = obterOuCriarGatePass(agendamento);
        try {
            validarStatusParaSaida(agendamento, gatePass);
            validarJanela(agendamento.getHorarioPrevistoSaida(), timestamp,
                    flowProperties.getToleranciaSaidaAntecipada(),
                    flowProperties.getToleranciaSaidaAtraso(),
                    "Horário de saída fora da tolerância permitida");

            agendamento.setHorarioRealSaida(timestamp);
            gatePass.setDataSaida(timestamp);
            gatePass.setStatus(StatusGate.FINALIZADO);

            if (agendamento.getHorarioRealChegada() == null) {
                agendamento.setStatus(StatusAgendamento.NO_SHOW);
            } else {
                agendamento.setStatus(StatusAgendamento.COMPLETO);
            }

            gatePassRepository.save(gatePass);
            agendamentoRepository.save(agendamento);

            GateEvent evento = registrarEvento(gatePass, StatusGate.FINALIZADO, null,
                    "Saída registrada", resolverOperador(request.getOperador()), timestamp);
            LOGGER.info("Saída registrada para agendamento {}", agendamento.getCodigo());

            agendamentoRealtimeService.notificarStatus(agendamento);
            return GateDecisionDTO.autorizado(evento.getStatus(), agendamento, gatePass,
                    "Saída registrada e gate finalizado");
        } catch (RuntimeException ex) {
            registrarEvento(gatePass, StatusGate.RETIDO, null, ex.getMessage(),
                    resolverOperador(request.getOperador()), timestamp);
            throw ex;
        }
    }

    public GateEvent registrarBloqueioManual(Long agendamentoId, ManualReleaseRequest request) {
        validarPermissaoManual(request.getOperador());
        Agendamento agendamento = obterAgendamento(agendamentoId);
        GatePass gatePass = obterOuCriarGatePass(agendamento);
        gatePass.setStatus(StatusGate.RETIDO);
        gatePassRepository.save(gatePass);
        return registrarEvento(gatePass, StatusGate.RETIDO, parseMotivo(request.getMotivo()),
                request.getObservacao(), resolverOperador(request.getOperador()), LocalDateTime.now());
    }

    public GateEvent registrarLiberacaoManual(Long agendamentoId, ManualReleaseRequest request) {
        validarPermissaoManual(request.getOperador());
        Agendamento agendamento = obterAgendamento(agendamentoId);
        GatePass gatePass = obterOuCriarGatePass(agendamento);
        gatePass.setStatus(StatusGate.LIBERADO);
        gatePassRepository.save(gatePass);
        return registrarEvento(gatePass, StatusGate.LIBERADO, parseMotivo(request.getMotivo()),
                request.getObservacao(), resolverOperador(request.getOperador()), LocalDateTime.now());
    }

    public br.com.cloudport.servicogate.dto.GateEventDTO liberarManual(Long agendamentoId, ManualReleaseRequest request) {
        GateEvent evento;
        if (request.getAcao() == ManualReleaseAction.LIBERAR) {
            evento = registrarLiberacaoManual(agendamentoId, request);
        } else {
            evento = registrarBloqueioManual(agendamentoId, request);
        }
        return GateMapper.toGateEventDTO(evento);
    }

    public TosSyncResponse sincronizarAgendamento(Long agendamentoId) {
        Agendamento agendamento = obterAgendamento(agendamentoId);
        TosSyncResponse sincronizacao = tosIntegrationService.sincronizar(agendamento);
        LOGGER.info("Sincronização solicitada para agendamento {}", agendamento.getCodigo());
        agendamentoRealtimeService.notificarStatus(agendamento);
        return sincronizacao;
    }

    public AgendamentoDTO confirmarChegadaAntecipada(Long agendamentoId) {
        Agendamento agendamento = obterAgendamento(agendamentoId);
        if (EnumSet.of(StatusAgendamento.CANCELADO, StatusAgendamento.NO_SHOW, StatusAgendamento.COMPLETO)
                .contains(agendamento.getStatus())) {
            throw new BusinessException("Agendamento não permite confirmação antecipada no status atual");
        }
        if (agendamento.getStatus() == StatusAgendamento.EM_EXECUCAO) {
            return GateMapper.toAgendamentoDTO(agendamento);
        }
        GatePass gatePass = obterOuCriarGatePass(agendamento);
        agendamento.setStatus(StatusAgendamento.EM_ATENDIMENTO);
        gatePass.setStatus(StatusGate.AGUARDANDO_ENTRADA);

        gatePassRepository.save(gatePass);
        agendamentoRepository.save(agendamento);

        registrarEvento(gatePass, StatusGate.AGUARDANDO_ENTRADA, null,
                "Chegada antecipada confirmada", resolverOperador(null), LocalDateTime.now());
        agendamentoRealtimeService.notificarStatus(agendamento);
        agendamentoRealtimeService.verificarJanelaProxima(agendamento);
        return GateMapper.toAgendamentoDTO(agendamento);
    }

    private Agendamento localizarAgendamento(GateFlowRequest request) {
        if (StringUtils.hasText(request.getQrCode())) {
            return agendamentoRepository.findByCodigo(request.getQrCode().trim())
                    .orElseThrow(() -> new NotFoundException("Agendamento não encontrado para o QR code informado"));
        }
        if (StringUtils.hasText(request.getPlaca())) {
            List<StatusAgendamento> status = new ArrayList<>(STATUS_VALIDOS_ENTRADA);
            return agendamentoRepository.findFirstByVeiculoPlacaIgnoreCaseAndStatusInOrderByHorarioPrevistoChegadaAsc(
                            request.getPlaca().trim(), status)
                    .orElseThrow(() -> new NotFoundException("Agendamento não encontrado para a placa informada"));
        }
        throw new BusinessException("Informe a placa ou o QR code para processar o evento");
    }

    private GatePass obterOuCriarGatePass(Agendamento agendamento) {
        GatePass gatePass = gatePassRepository.findByAgendamentoId(agendamento.getId()).orElse(null);
        if (gatePass == null) {
            gatePass = new GatePass();
            gatePass.setAgendamento(agendamento);
            gatePass.setCodigo(gerarCodigoGatePass(agendamento));
            gatePass.setToken(gerarTokenGatePass());
            gatePass.setStatus(StatusGate.AGUARDANDO_ENTRADA);
            gatePass = gatePassRepository.save(gatePass);
            agendamento.setGatePass(gatePass);
        } else if (agendamento.getGatePass() == null) {
            agendamento.setGatePass(gatePass);
        }
        if (!StringUtils.hasText(gatePass.getToken())) {
            gatePass.setToken(gerarTokenGatePass());
        }
        return gatePass;
    }

    private String gerarCodigoGatePass(Agendamento agendamento) {
        return "GP-" + agendamento.getCodigo() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String gerarTokenGatePass() {
        return UUID.randomUUID().toString();
    }

    private void validarStatusParaEntrada(Agendamento agendamento) {
        if (!STATUS_VALIDOS_ENTRADA.contains(agendamento.getStatus())) {
            throw new BusinessException("Agendamento não está elegível para entrada");
        }
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessException("Agendamento cancelado não pode ingressar no site");
        }
    }

    private void validarStatusParaSaida(Agendamento agendamento, GatePass gatePass) {
        if (!STATUS_VALIDOS_SAIDA.contains(agendamento.getStatus())) {
            throw new BusinessException("Agendamento não está em execução para registrar a saída");
        }
        if (gatePass.getDataEntrada() == null) {
            throw new BusinessException("Não é possível registrar saída sem entrada registrada");
        }
    }

    private void validarDocumentos(Agendamento agendamento) {
        List<DocumentoAgendamento> documentos = agendamento.getDocumentos();
        if (CollectionUtils.isEmpty(documentos)) {
            throw new BusinessException("Agendamento sem documentação obrigatória");
        }
        boolean documentoInvalido = documentos.stream()
                .anyMatch(doc -> !StringUtils.hasText(doc.getUrlDocumento()));
        if (documentoInvalido) {
            throw new BusinessException("Documentação pendente de validação");
        }
    }

    private void validarJanela(LocalDateTime horarioPrevisto,
                               LocalDateTime timestamp,
                               Duration toleranciaAntecipada,
                               Duration toleranciaAtraso,
                               String mensagemErro) {
        if (horarioPrevisto == null) {
            return;
        }
        LocalDateTime inicio = horarioPrevisto.minus(toleranciaAntecipada != null ? toleranciaAntecipada : Duration.ZERO);
        LocalDateTime fim = horarioPrevisto.plus(toleranciaAtraso != null ? toleranciaAtraso : Duration.ZERO);
        if (timestamp.isBefore(inicio) || timestamp.isAfter(fim)) {
            throw new BusinessException(mensagemErro);
        }
    }

    private GateEvent registrarEvento(GatePass gatePass,
                                      StatusGate status,
                                      MotivoExcecao motivo,
                                      String observacao,
                                      String operador,
                                      LocalDateTime timestamp) {
        GateEvent event = new GateEvent();
        event.setGatePass(gatePass);
        event.setStatus(status);
        event.setMotivoExcecao(motivo);
        event.setObservacao(observacao);
        event.setUsuarioResponsavel(operador);
        event.setRegistradoEm(timestamp != null ? timestamp : LocalDateTime.now());
        GateEvent salvo = gateEventRepository.save(event);
        gatePass.getEventos().add(salvo);
        agendamentoRealtimeService.notificarGatePass(gatePass);
        gateOperadorRealtimeService.publicarEvento(GateOperadorMapper.toEventoDTO(salvo));
        return salvo;
    }

    private MotivoExcecao parseMotivo(String motivo) {
        if (!StringUtils.hasText(motivo)) {
            return null;
        }
        try {
            String normalized = motivo.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return MotivoExcecao.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Motivo de exceção inválido: " + motivo);
        }
    }

    private Agendamento obterAgendamento(Long agendamentoId) {
        return agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado"));
    }

    private void validarPermissaoManual(String operador) {
        List<String> roles = flowProperties.getRolesLiberacaoManual();
        if (CollectionUtils.isEmpty(roles)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("Usuário não autorizado para liberação manual");
        }
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        boolean permitido = authorities.stream()
                .map(authority -> authority != null ? authority.toUpperCase(Locale.ROOT) : null)
                .filter(Objects::nonNull)
                .anyMatch(auth -> roles.stream()
                        .map(role -> role != null ? role.toUpperCase(Locale.ROOT) : null)
                        .filter(Objects::nonNull)
                        .anyMatch(auth::equals));
        if (!permitido) {
            throw new BusinessException("Usuário não possui permissão para liberação manual");
        }
    }

    private LocalDateTime resolverTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? timestamp : LocalDateTime.now();
    }

    private String resolverOperador(String operadorInformado) {
        if (StringUtils.hasText(operadorInformado)) {
            return operadorInformado.trim();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        return "sistema";
    }
}
