package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.cloudport.servicogate.integration.tos.TosIntegrationService;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.Veiculo;
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
class AnomalyDetectionTest {

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
    private Veiculo veiculo;

    @BeforeEach
    void setUp() {
        veiculo = new Veiculo();
        veiculo.setId(1L);
        veiculo.setPlaca("ABC1234");

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCodigo("CONT123456");
        agendamento.setVeiculo(veiculo);

        gatePass = new GatePass();
        gatePass.setId(1L);
        gatePass.setCodigo("GP-001");
        gatePass.setAgendamento(agendamento);
    }

    @Test
    void deveDetectarSaidaSemEntrada() {
        gatePass.setStatus(StatusGate.FINALIZADO);
        gatePass.setDataEntrada(null);
        gatePass.setDataSaida(LocalDateTime.now().minusMinutes(5));

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.FINALIZADO)).thenReturn(List.of(gatePass));
        when(gatePassRepository.findAll()).thenReturn(List.of(gatePass));
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.SAIDA_SEM_ENTRADA))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        assertTrue(resultado.stream()
                .anyMatch(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.SAIDA_SEM_ENTRADA));
        assertEquals(StatusGate.FINALIZADO.name(), resultado.get(0).getStatusLocal());
    }

    @Test
    void deveDetectarMultiplosContainersMesmaPlaca() {
        GatePass gatePass2 = new GatePass();
        gatePass2.setId(2L);
        gatePass2.setCodigo("GP-002");
        gatePass2.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass2.setAgendamento(agendamento);
        gatePass2.setDataEntrada(LocalDateTime.now().minusMinutes(30));
        gatePass2.setDataSaida(null);

        gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(50));
        gatePass.setDataSaida(null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(gatePassRepository.findAll()).thenReturn(List.of(gatePass, gatePass2));
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(anyLong(),
                eq(TipoDesincroniaBarcode.MULTIPLOS_CONTAINERS_PLACA)))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(System.nanoTime());
                    }
                    return r;
                });

        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        long multiploContainers = resultado.stream()
                .filter(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.MULTIPLOS_CONTAINERS_PLACA)
                .count();
        assertEquals(2, multiploContainers);
    }

    @Test
    void deveDetectarTempoGateExcedido() {
        gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(45));
        gatePass.setDataSaida(null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(gatePassRepository.findAll()).thenReturn(List.of(gatePass));
        when(reconciliacaoRepository.findByGatePassIdAndTipoDesinconia(1L,
                TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO))
                .thenReturn(Optional.empty());
        when(reconciliacaoRepository.save(any(ReconciliacaoBarcode.class)))
                .thenAnswer(inv -> {
                    ReconciliacaoBarcode r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        assertTrue(resultado.stream()
                .anyMatch(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO));
        ReconciliacaoBarcode anomalia = resultado.stream()
                .filter(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO)
                .findFirst()
                .orElseThrow();
        assertTrue(anomalia.getDescricao().contains("45 minutos"));
    }

    @Test
    void naoDeveDetectarMultiplosContainersDePlacasDiferentes() {
        Veiculo veiculo2 = new Veiculo();
        veiculo2.setId(2L);
        veiculo2.setPlaca("XYZ9876");

        Agendamento agendamento2 = new Agendamento();
        agendamento2.setId(2L);
        agendamento2.setCodigo("CONT654321");
        agendamento2.setVeiculo(veiculo2);

        GatePass gatePass2 = new GatePass();
        gatePass2.setId(2L);
        gatePass2.setCodigo("GP-002");
        gatePass2.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass2.setAgendamento(agendamento2);
        gatePass2.setDataEntrada(LocalDateTime.now().minusMinutes(10));
        gatePass2.setDataSaida(null);

        gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(20));
        gatePass.setDataSaida(null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(gatePassRepository.findAll()).thenReturn(List.of(gatePass, gatePass2));

        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        long multiploContainers = resultado.stream()
                .filter(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.MULTIPLOS_CONTAINERS_PLACA)
                .count();
        assertEquals(0, multiploContainers);
    }

    @Test
    void naoDeveDetectarTempoGateExcedidoMenorQue30Min() {
        gatePass.setStatus(StatusGate.EM_PROCESSAMENTO);
        gatePass.setDataEntrada(LocalDateTime.now().minusMinutes(15));
        gatePass.setDataSaida(null);

        when(gatePassRepository.findByStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE))
                .thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.EM_PROCESSAMENTO)).thenReturn(List.of());
        when(gatePassRepository.findByStatus(StatusGate.LIBERADO)).thenReturn(List.of());
        when(gatePassRepository.findAll()).thenReturn(List.of(gatePass));

        List<ReconciliacaoBarcode> resultado = reconciliacaoService.executarReconciliacao();

        long tempoExcedido = resultado.stream()
                .filter(r -> r.getTipoDesinconia() == TipoDesincroniaBarcode.TEMPO_GATE_EXCEDIDO)
                .count();
        assertEquals(0, tempoExcedido);
    }
}
