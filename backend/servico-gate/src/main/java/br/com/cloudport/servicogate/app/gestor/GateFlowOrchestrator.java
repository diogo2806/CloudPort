package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.GateCargaGeralRequest;
import br.com.cloudport.servicogate.app.gestor.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta.ReservaGateResposta;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta.ReservarGateRequest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GateFlowOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateFlowOrchestrator.class);
    private static final String ENTRADA = "ENTRADA";
    private static final String SAIDA = "SAIDA";

    private final GateFlowService gateFlowService;
    private final GateOperationsService gateOperationsService;
    private final CargaGeralGatePorta cargaGeralGatePorta;

    public GateFlowOrchestrator(
            GateFlowService gateFlowService,
            GateOperationsService gateOperationsService,
            CargaGeralGatePorta cargaGeralGatePorta) {
        this.gateFlowService = gateFlowService;
        this.gateOperationsService = gateOperationsService;
        this.cargaGeralGatePorta = cargaGeralGatePorta;
    }

    public GateDecisionDTO registrarEntrada(GateFlowRequest request) {
        return executar(request, ENTRADA);
    }

    public GateDecisionDTO registrarSaida(GateFlowRequest request) {
        return executar(request, SAIDA);
    }

    private GateDecisionDTO executar(GateFlowRequest request, String estagio) {
        validarContratoCargaGeral(request);
        ContextoReserva contexto = reservarCargaGeral(request, estagio);
        try {
            GateDecisionDTO decisao = ENTRADA.equals(estagio)
                    ? gateFlowService.registrarEntrada(request)
                    : gateFlowService.registrarSaida(request);

            validarAgendamentoDaReserva(decisao, contexto);
            if (decisao.isAutorizado()) {
                if (ENTRADA.equals(estagio)) {
                    gateOperationsService.registrarEntrada(request, decisao.getGatePassId());
                } else {
                    gateOperationsService.registrarSaida(request, decisao.getGatePassId());
                }
                contexto = confirmarSeAplicavel(contexto, estagio, request.getOperador());
            }
            anexarReserva(decisao, contexto);
            return decisao;
        } catch (RuntimeException exception) {
            compensarAposFalha(contexto, estagio, request.getOperador(), exception);
            throw exception;
        }
    }

    private void validarContratoCargaGeral(GateFlowRequest request) {
        if (request.getCargaGeral() != null
                && (request.getReservaCargaGeralId() != null || request.getCommandIdCargaGeral() != null)) {
            throw new BusinessException(
                    "Informe cargaGeral ou o contrato legado de reserva, sem combinar os dois formatos");
        }
        if (request.getReservaCargaGeralId() != null && request.getCommandIdCargaGeral() == null) {
            throw new BusinessException("Informe o commandId da reserva de carga geral");
        }
    }

    private ContextoReserva reservarCargaGeral(GateFlowRequest request, String estagioAtual) {
        GateCargaGeralRequest carga = request.getCargaGeral();
        if (carga == null) {
            if (request.getReservaCargaGeralId() == null) {
                return null;
            }
            return ContextoReserva.legado(
                    request.getReservaCargaGeralId(),
                    request.getCommandIdCargaGeral(),
                    estagioAtual);
        }

        String estagioConfirmacao = carga.getTipoMovimento()
                == GateCargaGeralRequest.TipoMovimentoCargaGeral.RETIRADA ? SAIDA : ENTRADA;
        String usuario = resolverUsuario(request.getOperador());
        ReservaGateResposta resposta = cargaGeralGatePorta.reservar(new ReservarGateRequest(
                carga.getCommandId(),
                carga.getAgendamentoCodigo(),
                carga.getBlNumero(),
                carga.getDeliveryOrder(),
                carga.getLoteId(),
                carga.getTipoMovimento().name(),
                estagioConfirmacao,
                carga.getQuantidade(),
                carga.getVolumeM3(),
                carga.getPesoKg(),
                usuario));
        if (resposta == null || resposta.id() == null) {
            throw new BusinessException("O serviço de carga geral não retornou a reserva criada");
        }
        if ("COMPENSADA".equalsIgnoreCase(resposta.status())) {
            throw new BusinessException(
                    "A reserva de carga geral já foi compensada; gere um novo commandId para repetir o fluxo");
        }
        return ContextoReserva.novo(carga.getCommandId(), resposta, estagioConfirmacao);
    }

    private ContextoReserva confirmarSeAplicavel(
            ContextoReserva contexto,
            String estagioAtual,
            String operador) {
        if (contexto == null || contexto.legado() || !contexto.estagioConfirmacao().equals(estagioAtual)) {
            return contexto;
        }
        ReservaGateResposta resposta = cargaGeralGatePorta.confirmar(
                contexto.reservaId(),
                derivarCommandId(contexto.commandIdBase(), "CONFIRMACAO"),
                estagioAtual,
                resolverUsuario(operador));
        return contexto.comResposta(resposta);
    }

    private void validarAgendamentoDaReserva(GateDecisionDTO decisao, ContextoReserva contexto) {
        if (contexto == null || contexto.agendamentoCodigo() == null || decisao.getCodigoAgendamento() == null) {
            return;
        }
        if (!contexto.agendamentoCodigo().equalsIgnoreCase(decisao.getCodigoAgendamento())) {
            throw new BusinessException("A reserva de carga geral pertence a outro agendamento");
        }
    }

    private void compensarAposFalha(
            ContextoReserva contexto,
            String estagioAtual,
            String operador,
            RuntimeException causa) {
        if (contexto == null || !deveCompensar(contexto, estagioAtual)) {
            return;
        }
        try {
            cargaGeralGatePorta.compensar(
                    contexto.reservaId(),
                    derivarCommandId(contexto.commandIdBase(), "COMPENSACAO"),
                    limitarMotivo(causa),
                    resolverUsuario(operador));
        } catch (RuntimeException compensationException) {
            LOGGER.error(
                    "Falha ao compensar reserva de carga geral {} após erro no Gate",
                    contexto.reservaId(),
                    compensationException);
            causa.addSuppressed(compensationException);
        }
    }

    private boolean deveCompensar(ContextoReserva contexto, String estagioAtual) {
        if (contexto.legado()) {
            return true;
        }
        return !"CONFIRMADA".equalsIgnoreCase(contexto.status())
                || contexto.estagioConfirmacao().equals(estagioAtual);
    }

    private void anexarReserva(GateDecisionDTO decisao, ContextoReserva contexto) {
        if (contexto == null) {
            return;
        }
        decisao.setReservaCargaGeralId(contexto.reservaId());
        decisao.setStatusReservaCargaGeral(contexto.status());
        decisao.setEstagioConfirmacaoCargaGeral(contexto.estagioConfirmacao());
    }

    private UUID derivarCommandId(UUID commandIdBase, String acao) {
        return UUID.nameUUIDFromBytes(
                (commandIdBase + ":" + acao).getBytes(StandardCharsets.UTF_8));
    }

    private String resolverUsuario(String operador) {
        if (operador == null || operador.isBlank()) {
            return "sistema";
        }
        return operador.trim();
    }

    private String limitarMotivo(RuntimeException exception) {
        String mensagem = exception.getMessage();
        if (mensagem == null || mensagem.isBlank()) {
            mensagem = exception.getClass().getSimpleName();
        }
        String motivo = "Compensação automática após falha no Gate: " + mensagem;
        return motivo.length() <= 1000 ? motivo : motivo.substring(0, 1000);
    }

    private record ContextoReserva(
            UUID reservaId,
            UUID commandIdBase,
            String agendamentoCodigo,
            String status,
            String estagioConfirmacao,
            boolean legado) {

        private static ContextoReserva novo(
                UUID commandIdBase,
                ReservaGateResposta resposta,
                String estagioConfirmacao) {
            return new ContextoReserva(
                    resposta.id(),
                    commandIdBase,
                    resposta.agendamentoCodigo(),
                    normalizar(resposta.status(), "RESERVADA"),
                    normalizar(resposta.estagioConfirmacao(), estagioConfirmacao),
                    false);
        }

        private static ContextoReserva legado(UUID reservaId, UUID commandId, String estagioConfirmacao) {
            return new ContextoReserva(
                    reservaId,
                    commandId,
                    null,
                    null,
                    estagioConfirmacao,
                    true);
        }

        private ContextoReserva comResposta(ReservaGateResposta resposta) {
            if (resposta == null) {
                return this;
            }
            return new ContextoReserva(
                    resposta.id() != null ? resposta.id() : reservaId,
                    commandIdBase,
                    resposta.agendamentoCodigo() != null ? resposta.agendamentoCodigo() : agendamentoCodigo,
                    normalizar(resposta.status(), status),
                    normalizar(resposta.estagioConfirmacao(), estagioConfirmacao),
                    legado);
        }

        private static String normalizar(String valor, String padrao) {
            return valor == null || valor.isBlank() ? padrao : valor.trim().toUpperCase(Locale.ROOT);
        }
    }
}
