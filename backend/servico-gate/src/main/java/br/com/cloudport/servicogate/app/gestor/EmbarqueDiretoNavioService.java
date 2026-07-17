package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRepository;
import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioRequest;
import br.com.cloudport.servicogate.app.gestor.dto.EmbarqueDiretoNavioResponse;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.porta.navio.EmbarqueDiretoNavioPorta;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class EmbarqueDiretoNavioService {

    private static final String MARCADOR_EVENTO = "EMBARQUE_DIRETO_GATE_NAVIO";

    private final AgendamentoRepository agendamentoRepository;
    private final GatePassRepository gatePassRepository;
    private final GateEventRepository gateEventRepository;
    private final AgendamentoRealtimeService agendamentoRealtimeService;
    private final GateOperadorRealtimeService gateOperadorRealtimeService;
    private final EmbarqueDiretoNavioPorta embarqueDiretoNavioPorta;

    public EmbarqueDiretoNavioService(AgendamentoRepository agendamentoRepository,
                                      GatePassRepository gatePassRepository,
                                      GateEventRepository gateEventRepository,
                                      AgendamentoRealtimeService agendamentoRealtimeService,
                                      GateOperadorRealtimeService gateOperadorRealtimeService,
                                      EmbarqueDiretoNavioPorta embarqueDiretoNavioPorta) {
        this.agendamentoRepository = agendamentoRepository;
        this.gatePassRepository = gatePassRepository;
        this.gateEventRepository = gateEventRepository;
        this.agendamentoRealtimeService = agendamentoRealtimeService;
        this.gateOperadorRealtimeService = gateOperadorRealtimeService;
        this.embarqueDiretoNavioPorta = embarqueDiretoNavioPorta;
    }

    public EmbarqueDiretoNavioResponse embarcar(EmbarqueDiretoNavioRequest request) {
        Agendamento agendamento = agendamentoRepository.findById(request.getAgendamentoId())
                .orElseThrow(() -> new NotFoundException("Agendamento não encontrado"));
        GatePass gatePass = gatePassRepository.findByAgendamentoId(agendamento.getId())
                .orElseThrow(() -> new BusinessException("A carreta ainda não possui entrada registrada no gate"));

        String marcador = marcador(request.getAtribuicaoEstivaId());
        boolean repeticao = gateEventRepository.existsByGatePassIdAndObservacaoStartingWith(gatePass.getId(), marcador);
        if (!repeticao) {
            validarFluxoAberto(agendamento, gatePass);
        } else if (gatePass.getStatus() != StatusGate.FINALIZADO
                || agendamento.getStatus() != StatusAgendamento.COMPLETO) {
            throw new BusinessException("O embarque direto já foi iniciado, mas o fechamento do gate está inconsistente");
        }

        LocalDateTime horarioSolicitado = request.getHorarioEmbarque() != null
                ? request.getHorarioEmbarque()
                : LocalDateTime.now();
        if (horarioSolicitado.isBefore(gatePass.getDataEntrada())) {
            throw new BusinessException("O horário de embarque não pode ser anterior à entrada da carreta no gate");
        }

        EmbarqueDiretoNavioPorta.Resultado resultado = embarqueDiretoNavioPorta.embarcar(
                new EmbarqueDiretoNavioPorta.Comando(
                        request.getAtribuicaoEstivaId(),
                        normalizarCodigo(agendamento.getCodigo()),
                        horarioSolicitado));

        validarRetornoNavio(agendamento, request, resultado);

        if (!repeticao) {
            LocalDateTime horarioEfetivo = resultado.getEmbarcadoEm();
            gatePass.setDataSaida(horarioEfetivo);
            gatePass.setStatus(StatusGate.FINALIZADO);
            agendamento.setHorarioRealSaida(horarioEfetivo);
            agendamento.setStatus(StatusAgendamento.COMPLETO);
            gatePassRepository.save(gatePass);
            agendamentoRepository.save(agendamento);
            registrarEvento(gatePass, marcador, resultado, resolverOperador(request));
        }

        agendamentoRealtimeService.notificarStatus(agendamento);
        agendamentoRealtimeService.notificarGatePass(gatePass);
        return montarResposta(agendamento, gatePass, resultado, repeticao);
    }

    private void validarFluxoAberto(Agendamento agendamento, GatePass gatePass) {
        if (agendamento.getStatus() != StatusAgendamento.EM_EXECUCAO) {
            throw new BusinessException("O agendamento deve estar em execução para embarcar diretamente no navio");
        }
        if (gatePass.getDataEntrada() == null) {
            throw new BusinessException("A entrada da carreta no gate ainda não foi registrada");
        }
        if (gatePass.getDataSaida() != null || gatePass.getStatus() == StatusGate.FINALIZADO) {
            throw new BusinessException("A carreta já teve a saída do gate finalizada");
        }
        if (gatePass.getStatus() != StatusGate.LIBERADO) {
            throw new BusinessException("A carreta precisa estar liberada no gate antes de seguir diretamente ao cais");
        }
        if (!StringUtils.hasText(agendamento.getCodigo())) {
            throw new BusinessException("O agendamento não possui o código do contêiner");
        }
    }

    private void validarRetornoNavio(Agendamento agendamento,
                                     EmbarqueDiretoNavioRequest request,
                                     EmbarqueDiretoNavioPorta.Resultado resultado) {
        if (resultado == null || resultado.getEmbarcadoEm() == null) {
            throw new BusinessException("O módulo de Navio não confirmou o embarque");
        }
        if (!request.getAtribuicaoEstivaId().equals(resultado.getAtribuicaoEstivaId())) {
            throw new BusinessException("O módulo de Navio confirmou uma atribuição de estiva diferente da solicitada");
        }
        if (!normalizarCodigo(agendamento.getCodigo()).equals(normalizarCodigo(resultado.getCodigoConteiner()))) {
            throw new BusinessException("O contêiner confirmado no navio é diferente do contêiner que passou pelo gate");
        }
    }

    private void registrarEvento(GatePass gatePass,
                                  String marcador,
                                  EmbarqueDiretoNavioPorta.Resultado resultado,
                                  String operador) {
        GateEvent evento = new GateEvent();
        evento.setGatePass(gatePass);
        evento.setStatus(StatusGate.FINALIZADO);
        evento.setUsuarioResponsavel(operador);
        evento.setRegistradoEm(resultado.getEmbarcadoEm());
        evento.setObservacao(marcador
                + "|plano=" + resultado.getPlanoEstivaId()
                + "|conteiner=" + resultado.getCodigoConteiner()
                + "|posicao=" + resultado.getBaia() + "-" + resultado.getFileira() + "-" + resultado.getCamada()
                + "|passou_pelo_patio=false");
        GateEvent salvo = gateEventRepository.save(evento);
        gatePass.getEventos().add(salvo);
        gateOperadorRealtimeService.publicarEvento(GateOperadorMapper.toEventoDTO(salvo));
    }

    private EmbarqueDiretoNavioResponse montarResposta(Agendamento agendamento,
                                                        GatePass gatePass,
                                                        EmbarqueDiretoNavioPorta.Resultado resultado,
                                                        boolean repeticao) {
        return new EmbarqueDiretoNavioResponse(
                agendamento.getId(),
                resultado.getCodigoConteiner(),
                gatePass.getId(),
                resultado.getAtribuicaoEstivaId(),
                resultado.getPlanoEstivaId(),
                resultado.getBaia(),
                resultado.getFileira(),
                resultado.getCamada(),
                gatePass.getDataEntrada(),
                resultado.getEmbarcadoEm(),
                gatePass.getDataSaida(),
                false,
                gatePass.getStatus().name(),
                agendamento.getStatus().name(),
                repeticao
                        ? "Embarque direto já confirmado anteriormente; resultado reapresentado sem duplicidade"
                        : "Contêiner embarcado diretamente do gate para o navio, sem passagem pelo pátio");
    }

    private String marcador(Long atribuicaoEstivaId) {
        return MARCADOR_EVENTO + "|atribuicao=" + atribuicaoEstivaId + "|";
    }

    private String normalizarCodigo(String codigo) {
        return StringUtils.hasText(codigo) ? codigo.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String resolverOperador(EmbarqueDiretoNavioRequest request) {
        if (StringUtils.hasText(request.getOperador())) {
            return request.getOperador().trim();
        }
        if (StringUtils.hasText(request.getUsuario())) {
            return request.getUsuario().trim();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        return "sistema";
    }
}
