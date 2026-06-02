package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.cloudport.servicogate.app.gestor.dto.TosContainerStatus;
import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliacaoBarcodeServiceTest {

    @Mock
    private GatePassRepository gatePassRepository;

    @Mock
    private ReconciliacaoBarcodeRepository reconciliacaoRepository;

    @Mock
    private TosIntegrationService tosIntegrationService;

    @InjectMocks
    private ReconciliacaoBarcodeService reconciliacaoService;

    private GatePass gatePass;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCodigo("CONT123456");

        gatePass = new GatePass();
        gatePass.setId(1L);
        gatePass.setCodigo("GP-001");
        gatePass.setAgendamento(agendamento);
    }

    @Test
    void deveDetectarContainerPreso() {
        // Setup: Container com mais de 30 minutos em AGUARDANDO_CONFIRMACAO_BARCODE
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(45));

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of(gatePass));
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        // Verify
        assertEquals(1, resultado.size());
        assertEquals(TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO, resultado.get(0).getTipoDesinconia());
        assertEquals(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE.name(), resultado.get(0).getStatusLocal());
        assertTrue(resultado.get(0).getDescricao().contains("45 minutos"));
    }

    @Test
    void deveDetectarEntradaPresa24Horas() {
        // Setup: Container na entrada EM_PROCESSAMENTO há mais de 24 horas
        gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass.setDataEntrada(LocalDateTime.now().minusHours(25));
        gatePass.setDataSaida(null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO))
                .thenReturn(List.of(gatePass));
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.ENTRADA_SEM_SAIDA_24H))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    r.setId(2L);
                    return r;
                });

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        // Verify
        assertEquals(1, resultado.size());
        assertEquals(TipoDesincroniaBarcode.ENTRADA_SEM_SAIDA_24H, resultado.get(0).getTipoDesinconia());
        assertEquals(25, resultado.get(0).getTempoPendenciaHoras());
    }

    @Test
    void deveDetectarBarcodeMismatch() {
        // Setup: Barcode confirmado localmente diferente do TOS
        gatePass.setStatus(StatusGate.LIBERADO);
        gatePass.setCodigoBarcode("CONT-LOCAL-123");
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.CONFIRMADO);

        TosContainerStatus tosStatus = new TosContainerStatus("CONT-TOS-456", "ATIVO",
                true, true, LocalDateTime.now(), null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of(gatePass));
        when(tosIntegrationService.obterStatusContainer("CONT123456")).thenReturn(tosStatus);
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.BARCODE_MISMATCH))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    r.setId(3L);
                    return r;
                });

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        // Verify
        assertEquals(1, resultado.size());
        assertEquals(TipoDesincroniaBarcode.BARCODE_MISMATCH, resultado.get(0).getTipoDesinconia());
        assertEquals("CONT-LOCAL-123", resultado.get(0).getBarcodeRecebido());
        assertEquals("CONT-TOS-456", resultado.get(0).getBarcodeEsperado());
    }

    @Test
    void naoDeveDuplicarDesincroniaJaExistente() {
        // Setup: Desincronização já registrada
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(45));

        ReconciliacaoBarcode existente = new ReconciliacaoBarcode();
        existente.setId(99L);
        existente.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of(gatePass));
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO))
                .thenReturn(Optional.of(existente));

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        // Verify
        assertEquals(0, resultado.size());
        verify(reconciliacaoRepository, never()).save(any());
    }

    @Test
    void deveResolverDesinconia() {
        // Setup
        ReconciliacaoBarcode reconciliacao = new ReconciliacaoBarcode();
        reconciliacao.setId(1L);
        reconciliacao.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_NAO_CONFIRMADO);

        when(reconciliacaoRepository.findById(1L)).thenReturn(Optional.of(reconciliacao));
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class))).thenReturn(reconciliacao);

        // Execute
        reconciliacaoService.resolverDesinconia(1L, "Container retirado manualmente do pátio");

        // Verify
        verify(reconciliacaoRepository).save(argThat(r ->
                r.getResolvidoEm() != null &&
                "Container retirado manualmente do pátio".equals(r.getResolucao())
        ));
    }

    @Test
    void deveListarNaoResolvidas() {
        // Setup
        ReconciliacaoBarcode problema1 = new ReconciliacaoBarcode();
        problema1.setId(1L);

        ReconciliacaoBarcode problema2 = new ReconciliacaoBarcode();
        problema2.setId(2L);

        when(reconciliacaoRepository.findByResolvidoEmIsNull())
                .thenReturn(List.of(problema1, problema2));

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService.listarNaoResolvidas();

        // Verify
        assertEquals(2, resultado.size());
    }

    @Test
    void deveListarPorTipo() {
        // Setup
        ReconciliacaoBarcode problema = new ReconciliacaoBarcode();
        problema.setId(1L);
        problema.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_MISMATCH);

        when(reconciliacaoRepository.findByTipoDesinconia(TipoDesincroniaBarcode.BARCODE_MISMATCH))
                .thenReturn(List.of(problema));

        // Execute
        List<ReconciliacaoBarcode> resultado = reconciliacaoService
                .listarPorTipo(TipoDesincroniaBarcode.BARCODE_MISMATCH);

        // Verify
        assertEquals(1, resultado.size());
        assertEquals(TipoDesincroniaBarcode.BARCODE_MISMATCH, resultado.get(0).getTipoDesinconia());
    }
}
