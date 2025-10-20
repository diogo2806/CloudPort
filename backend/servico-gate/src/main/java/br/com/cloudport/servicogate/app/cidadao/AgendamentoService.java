package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.config.AgendamentoRulesProperties;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.AgendamentoRequest;
import br.com.cloudport.servicogate.app.cidadao.dto.DocumentoAgendamentoDTO;
import br.com.cloudport.servicogate.app.cidadao.dto.DocumentoUploadRequest;
import br.com.cloudport.servicogate.app.cidadao.dto.mapper.GateMapper;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.integration.ocr.ProcessamentoOcrPublisher;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.JanelaAtendimento;
import br.com.cloudport.servicogate.model.Motorista;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRepository;
import br.com.cloudport.servicogate.app.cidadao.DocumentoAgendamentoRepository;
import br.com.cloudport.servicogate.app.cidadao.JanelaAtendimentoRepository;
import br.com.cloudport.servicogate.app.cidadao.MotoristaRepository;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.app.cidadao.VeiculoRepository;
import br.com.cloudport.servicogate.storage.DocumentoStorageService;
import br.com.cloudport.servicogate.storage.StoredDocumento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

@Service
@Transactional
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final JanelaAtendimentoRepository janelaAtendimentoRepository;
    private final TransportadoraRepository transportadoraRepository;
    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final DocumentoAgendamentoRepository documentoAgendamentoRepository;
    private final DocumentoStorageService documentoStorageService;
    private final AgendamentoRulesProperties rulesProperties;
    private final TosIntegrationService tosIntegrationService;
    private final DashboardService dashboardService;
    private final AgendamentoRealtimeService agendamentoRealtimeService;
    private final ProcessamentoOcrPublisher processamentoOcrPublisher;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgendamentoService.class);

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              JanelaAtendimentoRepository janelaAtendimentoRepository,
                              TransportadoraRepository transportadoraRepository,
                              MotoristaRepository motoristaRepository,
                              VeiculoRepository veiculoRepository,
                              DocumentoAgendamentoRepository documentoAgendamentoRepository,
                              DocumentoStorageService documentoStorageService,
                              AgendamentoRulesProperties rulesProperties,
                              TosIntegrationService tosIntegrationService,
                              DashboardService dashboardService,
                              AgendamentoRealtimeService agendamentoRealtimeService,
                              ProcessamentoOcrPublisher processamentoOcrPublisher) {
        this.agendamentoRepository = agendamentoRepository;
        this.janelaAtendimentoRepository = janelaAtendimentoRepository;
        this.transportadoraRepository = transportadoraRepository;
        this.motoristaRepository = motoristaRepository;
        this.veiculoRepository = veiculoRepository;
        this.documentoAgendamentoRepository = documentoAgendamentoRepository;
        this.documentoStorageService = documentoStorageService;
        this.rulesProperties = rulesProperties;
        this.tosIntegrationService = tosIntegrationService;
        this.dashboardService = dashboardService;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
        this.processamentoOcrPublisher = processamentoOcrPublisher;
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoDTO> buscar(LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        Page<Agendamento> page;
        if (dataInicio != null && dataFim != null) {
            page = agendamentoRepository.findByJanelaAtendimentoDataBetween(dataInicio, dataFim, pageable);
        } else if (dataInicio != null) {
            page = agendamentoRepository.findByJanelaAtendimentoDataGreaterThanEqual(dataInicio, pageable);
        } else if (dataFim != null) {
            page = agendamentoRepository.findByJanelaAtendimentoDataLessThanEqual(dataFim, pageable);
        } else {
            page = agendamentoRepository.findAll(pageable);
        }
        return page.map(GateMapper::toAgendamentoDTO);
    }

    @Transactional(readOnly = true)
    public AgendamentoDTO buscarPorId(Long id) {
        Agendamento agendamento = obterAgendamento(id);
        agendamentoRealtimeService.verificarJanelaProxima(agendamento);
        return GateMapper.toAgendamentoDTO(agendamento);
    }

    public AgendamentoDTO criar(AgendamentoRequest request) {
        validarCodigoUnico(null, request.getCodigo());
        JanelaAtendimento janela = buscarJanela(request.getJanelaAtendimentoId());
        validarCapacidade(janela, null);
        validarRegrasDeHorario(janela, request.getHorarioPrevistoChegada());
        validarIntervaloPrevisto(request.getHorarioPrevistoChegada(), request.getHorarioPrevistoSaida());
        if (!horarioDentroDaJanela(janela, request.getHorarioPrevistoChegada())) {
            throw new BusinessException("Horário previsto de chegada deve estar dentro da janela de atendimento");
        }
        if (!horarioDentroDaJanela(janela, request.getHorarioPrevistoSaida())) {
            throw new BusinessException("Horário previsto de saída deve estar dentro da janela de atendimento");
        }

        TipoOperacao tipoOperacao = parseTipoOperacao(request.getTipoOperacao());
        tosIntegrationService.validarAgendamentoParaCriacao(request.getCodigo(), tipoOperacao);
        LOGGER.info("event=tos.validation.booking codigo={} tipoOperacao={}", request.getCodigo(), tipoOperacao);

        Agendamento agendamento = new Agendamento();
        aplicarDados(agendamento, request, janela);
        Agendamento salvo = agendamentoRepository.save(agendamento);
        agendamentoRepository.flush();
        dashboardService.publicarResumoGeral();
        agendamentoRealtimeService.notificarStatus(salvo);
        return GateMapper.toAgendamentoDTO(salvo);
    }

    public AgendamentoDTO atualizar(Long id, AgendamentoRequest request) {
        Agendamento existente = obterAgendamento(id);
        validarEdicaoPermitida(existente);
        validarCodigoUnico(id, request.getCodigo());

        JanelaAtendimento janela = buscarJanela(request.getJanelaAtendimentoId());
        if (!Objects.equals(janela.getId(), existente.getJanelaAtendimento().getId())) {
            validarCapacidade(janela, id);
            validarRegrasDeHorario(janela, request.getHorarioPrevistoChegada());
        } else {
            validarCapacidade(janela, id);
        }
        validarIntervaloPrevisto(request.getHorarioPrevistoChegada(), request.getHorarioPrevistoSaida());
        if (!horarioDentroDaJanela(janela, request.getHorarioPrevistoChegada())) {
            throw new BusinessException("Horário previsto de chegada deve estar dentro da janela de atendimento");
        }
        if (!horarioDentroDaJanela(janela, request.getHorarioPrevistoSaida())) {
            throw new BusinessException("Horário previsto de saída deve estar dentro da janela de atendimento");
        }

        aplicarDados(existente, request, janela);
        Agendamento salvo = agendamentoRepository.save(existente);
        agendamentoRepository.flush();
        dashboardService.publicarResumoGeral();
        agendamentoRealtimeService.notificarStatus(salvo);
        return GateMapper.toAgendamentoDTO(salvo);
    }

    public void cancelar(Long id) {
        Agendamento agendamento = obterAgendamento(id);
        validarEdicaoPermitida(agendamento);
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
        agendamentoRepository.flush();
        dashboardService.publicarResumoGeral();
        agendamentoRealtimeService.notificarStatus(agendamento);
    }

    public DocumentoAgendamentoDTO adicionarDocumento(Long agendamentoId, DocumentoUploadRequest request, MultipartFile arquivo) {
        Agendamento agendamento = obterAgendamento(agendamentoId);
        validarEdicaoPermitida(agendamento);
        StoredDocumento storedDocumento = documentoStorageService.armazenar(agendamentoId, arquivo);

        DocumentoAgendamento documento = new DocumentoAgendamento();
        documento.setAgendamento(agendamento);
        documento.setTipoDocumento(sanitizarTexto(resolverTipoDocumento(request, arquivo)));
        documento.setNumero(sanitizarTexto(request != null ? request.getNumero() : null));
        documento.setUrlDocumento(storedDocumento.getStorageKey());
        documento.setNomeArquivo(sanitizarTexto(storedDocumento.getNomeOriginal()));
        documento.setContentType(storedDocumento.getContentType());
        documento.setTamanhoBytes(storedDocumento.getTamanho());
        documento.setUltimaRevalidacao(null);
        documento.setStatusValidacao(StatusValidacaoDocumento.PROCESSANDO);
        documento.setMensagemValidacao("Documento enviado para validação automática via OCR.");

        DocumentoAgendamento salvo = documentoAgendamentoRepository.save(documento);
        agendamento.getDocumentos().add(salvo);
        agendamentoRealtimeService.notificarDocumentosAtualizados(agendamento);
        try {
            processamentoOcrPublisher.enfileirarProcessamento(salvo);
        } catch (RuntimeException ex) {
            LOGGER.error("Falha ao enfileirar documento para OCR", ex);
            salvo.setStatusValidacao(StatusValidacaoDocumento.FALHA);
            salvo.setMensagemValidacao("Não foi possível iniciar a validação automática. Tente novamente em instantes.");
            documentoAgendamentoRepository.save(salvo);
            agendamentoRealtimeService.notificarDocumentosAtualizados(agendamento);
        }
        return GateMapper.toDocumentoAgendamentoDTO(salvo);
    }

    @Transactional(readOnly = true)
    public DocumentoDownload baixarDocumento(Long agendamentoId, Long documentoId) {
        Agendamento agendamento = obterAgendamento(agendamentoId);
        DocumentoAgendamento documento = documentoAgendamentoRepository.findById(documentoId)
                .orElseThrow(() -> new NotFoundException("Documento não encontrado"));
        if (!Objects.equals(documento.getAgendamento().getId(), agendamento.getId())) {
            throw new NotFoundException("Documento não pertence ao agendamento informado");
        }
        org.springframework.core.io.Resource resource = documentoStorageService.carregarComoResource(documento.getUrlDocumento());
        if (!resource.exists()) {
            throw new NotFoundException("Arquivo do documento não encontrado");
        }
        String contentType = documento.getContentType() != null ? documento.getContentType() : determinarContentType(resource);
        String filename = documento.getNomeArquivo() != null ? documento.getNomeArquivo() : documento.getTipoDocumento();
        return new DocumentoDownload(resource, filename, contentType);
    }

    @Transactional(readOnly = true)
    public List<DocumentoAgendamentoDTO> listarDocumentos(Long agendamentoId) {
        Agendamento agendamento = obterAgendamento(agendamentoId);
        return documentoAgendamentoRepository.findByAgendamentoId(agendamento.getId()).stream()
                .map(GateMapper::toDocumentoAgendamentoDTO)
                .collect(Collectors.toList());
    }

    private String sanitizarTexto(String valor) {
        if (!StringUtils.hasText(valor)) {
            return null;
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFKC);
        String semCaracteresPerigosos = normalizado.replaceAll("[<>\"'`]", "");
        return semCaracteresPerigosos.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "").trim();
    }

    private String resolverTipoDocumento(DocumentoUploadRequest request, MultipartFile arquivo) {
        if (request != null && StringUtils.hasText(request.getTipoDocumento())) {
            return request.getTipoDocumento();
        }
        if (arquivo != null) {
            if (StringUtils.hasText(arquivo.getContentType())) {
                return arquivo.getContentType();
            }
            String originalFilename = arquivo.getOriginalFilename();
            if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
                String extensao = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
                return extensao.toUpperCase(Locale.ROOT);
            }
        }
        return "ARQUIVO";
    }

    private String determinarContentType(org.springframework.core.io.Resource resource) {
        try {
            if (resource.isFile()) {
                Path path = resource.getFile().toPath();
                return Files.probeContentType(path);
            }
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }

    private void aplicarDados(Agendamento agendamento, AgendamentoRequest request, JanelaAtendimento janela) {
        agendamento.setCodigo(request.getCodigo());
        agendamento.setTipoOperacao(parseTipoOperacao(request.getTipoOperacao()));
        agendamento.setStatus(parseStatus(request.getStatus()));
        agendamento.setTransportadora(buscarTransportadora(request.getTransportadoraId()));
        agendamento.setMotorista(buscarMotorista(request.getMotoristaId()));
        agendamento.setVeiculo(buscarVeiculo(request.getVeiculoId()));
        agendamento.setJanelaAtendimento(janela);
        agendamento.setHorarioPrevistoChegada(request.getHorarioPrevistoChegada());
        agendamento.setHorarioPrevistoSaida(request.getHorarioPrevistoSaida());
        agendamento.setObservacoes(request.getObservacoes());
    }

    private void validarCodigoUnico(Long id, String codigo) {
        agendamentoRepository.findByCodigo(codigo).ifPresent(existing -> {
            if (id == null || !existing.getId().equals(id)) {
                throw new BusinessException("Código de agendamento já utilizado");
            }
        });
    }

    private void validarCapacidade(JanelaAtendimento janela, Long agendamentoId) {
        long ocupacao;
        if (agendamentoId == null) {
            ocupacao = agendamentoRepository.countByJanelaAtendimentoIdAndStatusNot(janela.getId(), StatusAgendamento.CANCELADO);
        } else {
            ocupacao = agendamentoRepository.countByJanelaAtendimentoIdAndStatusNotAndIdNot(janela.getId(), StatusAgendamento.CANCELADO, agendamentoId);
        }
        if (ocupacao >= janela.getCapacidade()) {
            throw new BusinessException("Capacidade da janela atingida");
        }
    }

    private void validarRegrasDeHorario(JanelaAtendimento janela, LocalDateTime horarioPrevistoChegada) {
        LocalDateTime inicioJanela = LocalDateTime.of(janela.getData(), janela.getHoraInicio());
        LocalDateTime limiteCriacao = inicioJanela.minus(rulesProperties.getAntecedenciaMinima());
        LocalDateTime limiteAtraso = inicioJanela.plus(rulesProperties.getAtrasoMaximo());
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isAfter(limiteCriacao)) {
            throw new BusinessException("Agendamento deve ser criado com antecedência mínima de "
                    + rulesProperties.getAntecedenciaMinima().toHours() + " horas");
        }
        if (agora.isAfter(limiteAtraso)) {
            throw new BusinessException("Prazo para criação do agendamento expirou");
        }
        if (!horarioDentroDaJanela(janela, horarioPrevistoChegada)) {
            throw new BusinessException("Horário previsto de chegada deve estar dentro da janela de atendimento");
        }
    }

    private void validarIntervaloPrevisto(LocalDateTime chegada, LocalDateTime saida) {
        if (saida.isBefore(chegada) || saida.equals(chegada)) {
            throw new BusinessException("Horário previsto de saída deve ser posterior ao horário de chegada");
        }
    }

    private boolean horarioDentroDaJanela(JanelaAtendimento janela, LocalDateTime horario) {
        LocalDateTime inicio = LocalDateTime.of(janela.getData(), janela.getHoraInicio());
        LocalDateTime fim = LocalDateTime.of(janela.getData(), janela.getHoraFim());
        return (horario.isAfter(inicio) || horario.isEqual(inicio))
                && (horario.isBefore(fim) || horario.isEqual(fim));
    }

    private void validarEdicaoPermitida(Agendamento agendamento) {
        JanelaAtendimento janela = agendamento.getJanelaAtendimento();
        LocalDateTime inicio = LocalDateTime.of(janela.getData(), janela.getHoraInicio());
        LocalDateTime limiteAnterior = inicio.minus(rulesProperties.getEdicaoAntecedencia());
        LocalDateTime limitePosterior = inicio.plus(rulesProperties.getEdicaoAtraso());
        LocalDateTime agora = LocalDateTime.now();
        if (agora.isBefore(limiteAnterior) || agora.isAfter(limitePosterior)) {
            throw new BusinessException("Edição bloqueada para esta janela de atendimento");
        }
    }

    private Agendamento obterAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado"));
    }

    private JanelaAtendimento buscarJanela(Long id) {
        return janelaAtendimentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Janela de atendimento não encontrada"));
    }

    private Transportadora buscarTransportadora(Long id) {
        return transportadoraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transportadora não encontrada"));
    }

    private Motorista buscarMotorista(Long id) {
        return motoristaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Motorista não encontrado"));
    }

    private Veiculo buscarVeiculo(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Veículo não encontrado"));
    }

    private TipoOperacao parseTipoOperacao(String tipoOperacao) {
        try {
            String normalized = tipoOperacao.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return TipoOperacao.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Tipo de operação inválido: " + tipoOperacao);
        }
    }

    private StatusAgendamento parseStatus(String status) {
        try {
            String normalized = status.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return StatusAgendamento.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Status de agendamento inválido: " + status);
        }
    }
}
