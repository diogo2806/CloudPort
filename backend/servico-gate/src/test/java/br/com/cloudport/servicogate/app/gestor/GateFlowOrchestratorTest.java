package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.gestor.dto.GateCargaGeralRequest;
import br.com.cloudport.servicogate.app.gestor.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta;
import br.com.cloudport.servicogate.integration.cargageral.CargaGeralGatePorta.ReservaGateResposta;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateFlowOrchestratorTest {

    @Mock
    private GateFlowService gateFlowService;

    @Mock
    private GateOperationsService gateOperationsService;

    @Mock
    private CargaGeralGatePorta cargaGeralGatePorta;

    private GateFlowOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new GateFlowOrchestrator(
                gateFlowService,
                gateOperationsService,
                cargaGeralGatePorta);
    }

    @Test
    void deveReservarEConfirmarEntregaParcialNaEntrada() {
        GateFlowRequest request = criarRequest(GateCargaGeralRequest.TipoMovimentoCargaGeral.ENTREGA);
        UUID reservaId = UUID.randomUUID();
        when(cargaGeralGatePorta.reservar(any())).thenReturn(
                new ReservaGateResposta(reservaId, "AG-001", "RESERVADA", "ENTRADA"));
        when(gateFlowService.registrarEntrada(request)).thenReturn(decisaoAutorizada());
        when(cargaGeralGatePorta.confirmar(
                eq(reservaId), any(UUID.class), eq("ENTRADA"), eq("operador")))
                .thenReturn(new ReservaGateResposta(reservaId, "AG-001", "CONFIRMADA", "ENTRADA"));

        GateDecisionDTO resposta = orchestrator.registrarEntrada(request);

        verify(gateOperationsService).registrarEntrada(request, 10L);
        verify(cargaGeralGatePorta).confirmar(
                eq(reservaId), any(UUID.class), eq("ENTRADA"), eq("operador"));
        assertEquals(reservaId, resposta.getReservaCargaGeralId());
        assertEquals("CONFIRMADA", resposta.getStatusReservaCargaGeral());
        assertEquals("ENTRADA", resposta.getEstagioConfirmacaoCargaGeral());
    }

    @Test
    void deveManterRetiradaParcialReservadaAteASaida() {
        GateFlowRequest request = criarRequest(GateCargaGeralRequest.TipoMovimentoCargaGeral.RETIRADA);
        UUID reservaId = UUID.randomUUID();
        when(cargaGeralGatePorta.reservar(any())).thenReturn(
                new ReservaGateResposta(reservaId, "AG-001", "RESERVADA", "SAIDA"));
        when(gateFlowService.registrarEntrada(request)).thenReturn(decisaoAutorizada());

        GateDecisionDTO resposta = orchestrator.registrarEntrada(request);

        verify(cargaGeralGatePorta, never()).confirmar(any(), any(), any(), any());
        assertEquals("RESERVADA", resposta.getStatusReservaCargaGeral());
        assertEquals("SAIDA", resposta.getEstagioConfirmacaoCargaGeral());
    }

    @Test
    void deveConfirmarRetiradaParcialSomenteNaSaida() {
        GateFlowRequest request = criarRequest(GateCargaGeralRequest.TipoMovimentoCargaGeral.RETIRADA);
        UUID reservaId = UUID.randomUUID();
        when(cargaGeralGatePorta.reservar(any())).thenReturn(
                new ReservaGateResposta(reservaId, "AG-001", "RESERVADA", "SAIDA"));
        when(gateFlowService.registrarSaida(request)).thenReturn(decisaoAutorizada());
        when(cargaGeralGatePorta.confirmar(
                eq(reservaId), any(UUID.class), eq("SAIDA"), eq("operador")))
                .thenReturn(new ReservaGateResposta(reservaId, "AG-001", "CONFIRMADA", "SAIDA"));

        GateDecisionDTO resposta = orchestrator.registrarSaida(request);

        verify(gateOperationsService).registrarSaida(request, 10L);
        assertEquals("CONFIRMADA", resposta.getStatusReservaCargaGeral());
    }

    @Test
    void deveCompensarReservaQuandoFluxoLocalFalhar() {
        GateFlowRequest request = criarRequest(GateCargaGeralRequest.TipoMovimentoCargaGeral.ENTREGA);
        UUID reservaId = UUID.randomUUID();
        when(cargaGeralGatePorta.reservar(any())).thenReturn(
                new ReservaGateResposta(reservaId, "AG-001", "RESERVADA", "ENTRADA"));
        when(gateFlowService.registrarEntrada(request)).thenReturn(decisaoAutorizada());
        org.mockito.Mockito.doThrow(new BusinessException("Falha na fila do Gate"))
                .when(gateOperationsService).registrarEntrada(request, 10L);

        assertThrows(BusinessException.class, () -> orchestrator.registrarEntrada(request));

        verify(cargaGeralGatePorta).compensar(
                eq(reservaId),
                any(UUID.class),
                eq("Compensação automática após falha no Gate: Falha na fila do Gate"),
                eq("operador"));
    }

    private GateFlowRequest criarRequest(GateCargaGeralRequest.TipoMovimentoCargaGeral tipoMovimento) {
        GateCargaGeralRequest cargaGeral = new GateCargaGeralRequest();
        cargaGeral.setCommandId(UUID.randomUUID());
        cargaGeral.setAgendamentoCodigo("AG-001");
        cargaGeral.setBlNumero("BL-001");
        cargaGeral.setDeliveryOrder("DO-001");
        cargaGeral.setLoteId(UUID.randomUUID());
        cargaGeral.setTipoMovimento(tipoMovimento);
        cargaGeral.setQuantidade(new BigDecimal("10.000"));
        cargaGeral.setVolumeM3(new BigDecimal("2.500"));
        cargaGeral.setPesoKg(new BigDecimal("1200.000"));

        GateFlowRequest request = new GateFlowRequest();
        request.setQrCode("AG-001");
        request.setOperador("operador");
        request.setCargaGeral(cargaGeral);
        return request;
    }

    private GateDecisionDTO decisaoAutorizada() {
        GateDecisionDTO decisao = new GateDecisionDTO();
        decisao.setAutorizado(true);
        decisao.setCodigoAgendamento("AG-001");
        decisao.setGatePassId(10L);
        return decisao;
    }
}
