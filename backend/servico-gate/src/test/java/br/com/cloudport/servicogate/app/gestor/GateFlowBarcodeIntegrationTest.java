package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.cloudport.servicogate.app.gestor.dto.GateDecisionDTO;
import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.cidadao.AgendamentoRepository;
import br.com.cloudport.servicogate.config.BarcodeProperties;
import br.com.cloudport.servicogate.integration.dmt.DmtBarcodeService;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.DocumentoAgendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.Veiculo;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.app.gestor.dto.TosContainerStatus;
import br.com.cloudport.servicogate.config.GateFlowProperties;
import br.com.cloudport.servicogate.monitoring.GateMetrics;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GateFlowBarcodeIntegrationTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private GatePassRepository gatePassRepository;

    @Mock
    private GateEventRepository gateEventRepository;

    @Mock
    private GateFlowProperties flowProperties;

    @Mock
    private TosIntegrationService tosIntegrationService;

    @Mock
    private GateMetrics gateMetrics;

    @Mock
    private AgendamentoRealtimeService agendamentoRealtimeService;

    @Mock
    private GateOperadorRealtimeService gateOperadorRealtimeService;

    @Mock
    private DmtBarcodeService dmtBarcodeService;

    @Mock
    private BarcodeProperties barcodeProperties;

    @InjectMocks
    private GateFlowService gateFlowService;

    private Agendamento agendamento;
    private GatePass gatePass;
    private TosContainerStatus tosStatus;

    @BeforeEach
    void setUp() {
        // Setup agendamento
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCodigo("AG-001");
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamento.setHorarioPrevistoChegada(LocalDateTime.now().plus(Duration.ofMinutes(5)));

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca("ABC-1234");
        agendamento.setVeiculo(veiculo);

        DocumentoAgendamento doc = new DocumentoAgendamento();
        doc.setUrlDocumento("http://example.com/doc.pdf");
        agendamento.setDocumentos(java.util.List.of(doc));

        // Setup gatePass
        gatePass = new GatePass();
        gatePass.setId(1L);
        gatePass.setToken("token-xyz");
        gatePass.setCodigo("GP-001");
        gatePass.setAgendamento(agendamento);

        // Setup TOS status
        tosStatus = new TosContainerStatus("CONT123456", "ATIVO", true, true,
                LocalDateTime.now(), null);

        // Setup flow properties
        when(flowProperties.getToleranciaEntradaAntecipada()).thenReturn(Duration.ofMinutes(30));
        when(flowProperties.getToleranciaEntradaAtraso()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void deveRegistrarEntradaComValidacaoBarcodeHabilitada() {
        // Setup
        when(barcodeProperties.isHabilitado()).thenReturn(true);
        when(agendamentoRepository.findByCodigo("AG-001")).thenReturn(Optional.of(agendamento));
        when(gatePassRepository.findByAgendamentoId(1L)).thenReturn(Optional.empty());
        when(tosIntegrationService.validarParaEntrada(agendamento)).thenReturn(tosStatus);
        when(gatePassRepository.save(any(GatePass.class))).thenAnswer(inv -> {
            GatePass gp = inv.getArgument(0);
            gp.setId(1L);
            return gp;
        });
        when(gateEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GateFlowRequest request = new GateFlowRequest();
        request.setQrCode("AG-001");

        // Execute
        GateDecisionDTO decision = gateFlowService.registrarEntrada(request);

        // Verify
        assertNotNull(decision);
        assertFalse(decision.isAutorizado());
        assertEquals(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE.name(), decision.getStatusGate());
        assertNotNull(decision.getTokenGatePass());
        assertTrue(decision.getMensagem().contains("barcode"));

        // Verify barcode service was called
        verify(dmtBarcodeService).solicitarConfirmacaoBarcode(any(GatePass.class), eq("AG-001"));

        // Verify notifications
        verify(agendamentoRealtimeService).notificarStatus(agendamento);
    }

    @Test
    void deveRegistrarEntradaSemValidacaoBarcodeDesabilitada() {
        // Setup
        when(barcodeProperties.isHabilitado()).thenReturn(false);
        when(agendamentoRepository.findByCodigo("AG-001")).thenReturn(Optional.of(agendamento));
        when(gatePassRepository.findByAgendamentoId(1L)).thenReturn(Optional.empty());
        when(tosIntegrationService.validarParaEntrada(agendamento)).thenReturn(tosStatus);
        when(gatePassRepository.save(any(GatePass.class))).thenAnswer(inv -> {
            GatePass gp = inv.getArgument(0);
            gp.setId(1L);
            return gp;
        });
        when(gateEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GateFlowRequest request = new GateFlowRequest();
        request.setQrCode("AG-001");

        // Execute
        GateDecisionDTO decision = gateFlowService.registrarEntrada(request);

        // Verify
        assertNotNull(decision);
        assertTrue(decision.isAutorizado());
        assertEquals(StatusGate.LIBERADO.name(), decision.getStatusGate());

        // Verify barcode service was NOT called
        verify(dmtBarcodeService, never()).solicitarConfirmacaoBarcode(any(), any());

        // Verify notifications
        verify(agendamentoRealtimeService).notificarStatus(agendamento);
    }

    @Test
    void deveAtualizarStatusParaBarcodeConfirmadoViaWebhook() {
        // Setup
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.PENDENTE);

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Simulate DMT confirmation via webhook
        ConfirmacaoBarcodeService barcodeService = new ConfirmacaoBarcodeService(
                gatePassRepository, gateEventRepository, agendamentoRealtimeService);

        br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest request =
                new br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setCodigoBarcode("CONT123456");
        request.setConfirmado(true);
        request.setDispositivoDmtId("DMT-001");

        // Execute
        br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse response =
                barcodeService.confirmarBarcode(request);

        // Verify
        assertNotNull(response);
        assertEquals(StatusConfirmacaoBarcode.CONFIRMADO.toString(), response.getStatusConfirmacao());
        assertEquals("CONT123456", response.getCodigoBarcode());

        // Verify GatePass was updated
        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatus() == StatusGate.LIBERADO &&
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.CONFIRMADO &&
                "CONT123456".equals(gp.getCodigoBarcode())
        ));

        // Verify real-time notification
        verify(agendamentoRealtimeService).notificarStatus(agendamento);
    }

    @Test
    void deveAtualizarStatusParaBarcodeRejeitadoViaWebhook() {
        // Setup
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.PENDENTE);

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Simulate DMT rejection
        ConfirmacaoBarcodeService barcodeService = new ConfirmacaoBarcodeService(
                gatePassRepository, gateEventRepository, agendamentoRealtimeService);

        br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest request =
                new br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setCodigoBarcode("CONT-INCORRETO");
        request.setConfirmado(false);
        request.setMotivo("Barcode não corresponde");
        request.setDispositivoDmtId("DMT-001");

        // Execute
        br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse response =
                barcodeService.confirmarBarcode(request);

        // Verify
        assertNotNull(response);
        assertEquals(StatusConfirmacaoBarcode.REJEITADO.toString(), response.getStatusConfirmacao());

        // Verify GatePass was updated to RETIDO
        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatus() == StatusGate.RETIDO &&
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.REJEITADO &&
                "Barcode não corresponde".equals(gp.getMotivoRejeicaoBarcode())
        ));
    }

    @Test
    void deveHandlerTimeoutBarcodeConfirmacao() {
        // Setup
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.PENDENTE);

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmacaoBarcodeService barcodeService = new ConfirmacaoBarcodeService(
                gatePassRepository, gateEventRepository, agendamentoRealtimeService);

        // Execute timeout
        br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse response =
                barcodeService.registrarTimeoutBarcode("token-xyz", "DMT-001");

        // Verify
        assertNotNull(response);
        assertEquals(StatusConfirmacaoBarcode.TIMEOUT.toString(), response.getStatusConfirmacao());

        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatus() == StatusGate.RETIDO &&
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.TIMEOUT
        ));
    }
}
