package br.com.cloudport.servicogate.service;

import br.com.cloudport.servicogate.dto.ManualReleaseAction;
import br.com.cloudport.servicogate.dto.ManualReleaseRequest;
import br.com.cloudport.servicogate.dto.mapper.GateOperadorMapper;
import br.com.cloudport.servicogate.dto.operador.GateOperadorBloqueioRequest;
import br.com.cloudport.servicogate.dto.operador.GateOperadorComprovanteDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorEventoDTO;
import br.com.cloudport.servicogate.dto.operador.GateOperadorLiberacaoRequest;
import br.com.cloudport.servicogate.dto.operador.GateOperadorOcorrenciaRequest;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GateOcorrencia;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.TipoOcorrenciaOperador;
import br.com.cloudport.servicogate.repository.AgendamentoRepository;
import br.com.cloudport.servicogate.repository.GateOcorrenciaRepository;
import br.com.cloudport.servicogate.repository.VeiculoRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GateOperadorOperacoesService {

    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final GateFlowService gateFlowService;
    private final GateOperadorRealtimeService realtimeService;
    private final GateOcorrenciaRepository gateOcorrenciaRepository;
    private final VeiculoRepository veiculoRepository;

    public GateOperadorOperacoesService(AgendamentoRepository agendamentoRepository,
                                        GateFlowService gateFlowService,
                                        GateOperadorRealtimeService realtimeService,
                                        GateOcorrenciaRepository gateOcorrenciaRepository,
                                        VeiculoRepository veiculoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.gateFlowService = gateFlowService;
        this.realtimeService = realtimeService;
        this.gateOcorrenciaRepository = gateOcorrenciaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    public void liberarVeiculo(Long veiculoId, GateOperadorLiberacaoRequest request) {
        Agendamento agendamento = localizarAgendamentoAtivo(veiculoId);
        ManualReleaseRequest manual = new ManualReleaseRequest();
        manual.setAcao(ManualReleaseAction.LIBERAR);
        manual.setObservacao(formatarJustificativaLiberacao(request));
        manual.setOperador(obterOperadorAtual());
        manual.setMotivo(null);
        GateEvent evento = gateFlowService.registrarLiberacaoManual(agendamento.getId(), manual);
        realtimeService.publicarEvento(GateOperadorMapper.toEventoDTO(evento));
    }

    public void bloquearVeiculo(Long veiculoId, GateOperadorBloqueioRequest request) {
        Agendamento agendamento = localizarAgendamentoAtivo(veiculoId);
        ManualReleaseRequest manual = new ManualReleaseRequest();
        manual.setAcao(ManualReleaseAction.BLOQUEAR);
        manual.setMotivo(request.motivoCodigo());
        manual.setObservacao(formatarJustificativaBloqueio(request));
        manual.setOperador(obterOperadorAtual());
        GateEvent evento = gateFlowService.registrarBloqueioManual(agendamento.getId(), manual);
        realtimeService.publicarEvento(GateOperadorMapper.toEventoDTO(evento));
    }

    public GateOperadorEventoDTO registrarOcorrencia(GateOperadorOcorrenciaRequest request) {
        TipoOcorrenciaOperador tipo;
        try {
            tipo = TipoOcorrenciaOperador.fromCodigo(request.tipoCodigo());
        } catch (IllegalArgumentException ex) {
            throw new NotFoundException("Tipo de ocorrência informado é inválido");
        }
        Veiculo veiculo = null;
        if (request.veiculoId() != null) {
            veiculo = veiculoRepository.findById(request.veiculoId())
                    .orElseThrow(() -> new NotFoundException("Veículo informado na ocorrência não foi encontrado"));
        }
        GateOcorrencia ocorrencia = new GateOcorrencia();
        ocorrencia.setTipo(tipo);
        ocorrencia.setNivel(tipo.getNivelPadrao());
        ocorrencia.setDescricao(request.descricao());
        ocorrencia.setRegistradoEm(LocalDateTime.now());
        ocorrencia.setUsuarioResponsavel(obterOperadorAtual());
        ocorrencia.setVeiculo(veiculo);
        ocorrencia.setTransportadora(veiculo != null ? veiculo.getTransportadora() : null);
        GateOcorrencia salvo = gateOcorrenciaRepository.save(ocorrencia);
        GateOperadorEventoDTO evento = GateOperadorMapper.toEventoDTO(salvo);
        realtimeService.publicarEvento(evento);
        return evento;
    }

    @Transactional(readOnly = true)
    public GateOperadorComprovanteDTO gerarComprovante(Long veiculoId) {
        Agendamento agendamento = localizarAgendamentoAtivo(veiculoId);
        StringBuilder builder = new StringBuilder();
        builder.append("Comprovante de Gate\n");
        builder.append("Emitido em: ").append(LocalDateTime.now().format(DATA_HORA_FORMATTER)).append('\n');
        builder.append("Agendamento: ").append(agendamento.getCodigo()).append('\n');
        builder.append("Status: ").append(agendamento.getStatus() != null ? agendamento.getStatus().getDescricao() : "").append('\n');
        if (agendamento.getTransportadora() != null) {
            builder.append("Transportadora: ").append(agendamento.getTransportadora().getNome()).append('\n');
        }
        if (agendamento.getMotorista() != null) {
            builder.append("Motorista: ").append(agendamento.getMotorista().getNome()).append('\n');
        }
        if (agendamento.getVeiculo() != null) {
            builder.append("Placa: ").append(agendamento.getVeiculo().getPlaca()).append('\n');
        }
        if (agendamento.getHorarioPrevistoChegada() != null) {
            builder.append("Chegada prevista: ")
                    .append(agendamento.getHorarioPrevistoChegada().format(DATA_HORA_FORMATTER))
                    .append('\n');
        }
        if (agendamento.getHorarioRealChegada() != null) {
            builder.append("Chegada registrada: ")
                    .append(agendamento.getHorarioRealChegada().format(DATA_HORA_FORMATTER))
                    .append('\n');
        }
        if (agendamento.getHorarioRealSaida() != null) {
            builder.append("Saída registrada: ")
                    .append(agendamento.getHorarioRealSaida().format(DATA_HORA_FORMATTER))
                    .append('\n');
        }
        builder.append("Operador responsável: ").append(obterOperadorAtual()).append('\n');

        byte[] conteudo = builder.toString().getBytes(StandardCharsets.UTF_8);
        String nomeArquivo = "comprovante-gate-" +
                (agendamento.getVeiculo() != null ? agendamento.getVeiculo().getPlaca() : "veiculo") + ".txt";
        return new GateOperadorComprovanteDTO(nomeArquivo, conteudo, "text/plain;charset=UTF-8");
    }

    private Agendamento localizarAgendamentoAtivo(Long veiculoId) {
        List<StatusAgendamento> status = List.of(
                StatusAgendamento.CONFIRMADO,
                StatusAgendamento.EM_ATENDIMENTO,
                StatusAgendamento.EM_EXECUCAO
        );
        return agendamentoRepository.findFirstByVeiculoIdAndStatusInOrderByHorarioPrevistoChegadaDesc(veiculoId, status)
                .orElseThrow(() -> new NotFoundException("Não foi encontrado agendamento ativo para o veículo informado"));
    }

    private String formatarJustificativaLiberacao(GateOperadorLiberacaoRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.justificativa());
        if (StringUtils.hasText(request.canalEntrada())) {
            builder.append(" | Canal: ").append(request.canalEntrada());
        }
        if (Boolean.TRUE.equals(request.notificarTransportadora())) {
            builder.append(" | Transportadora notificada");
        }
        return builder.toString();
    }

    private String formatarJustificativaBloqueio(GateOperadorBloqueioRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.justificativa());
        if (StringUtils.hasText(request.bloqueioAte())) {
            builder.append(" | Bloqueio até: ").append(request.bloqueioAte());
        }
        return builder.toString();
    }

    private String obterOperadorAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
