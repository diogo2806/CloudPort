package br.com.cloudport.servicogate.app.gestor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.cloudport.servicogate.app.cidadao.AgendamentoRealtimeService;
import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeRequest;
import br.com.cloudport.servicogate.app.gestor.dto.ConfirmacaoBarcodeResponse;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.GateEvent;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.enums.StatusConfirmacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoBarcodeServiceTest {

    @Mock
    private GatePassRepository gatePassRepository;

    @Mock
    private GateEventRepository gateEventRepository;

    @Mock
    private AgendamentoRealtimeService agendamentoRealtimeService;

    @InjectMocks
    private ConfirmacaoBarcodeService service;

    private GatePass gatePass;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCodigo("AG-001");

        gatePass = new GatePass();
        gatePass.setId(1L);
        gatePass.setToken("token-xyz");
        gatePass.setCodigo("GP-001");
        gatePass.setStatus(StatusGate.AGUARDANDO_CONFIRMACAO_BARCODE);
        gatePass.setAgendamento(agendamento);
    }

    @Test
    void deveConfirmarBarcodeComSucesso() {
        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setCodigoBarcode("CONT123456");
        request.setConfirmado(true);
        request.setDispositivoDmtId("DMT-001");

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any(GateEvent.class))).thenReturn(new GateEvent());

        ConfirmacaoBarcodeResponse resposta = service.confirmarBarcode(request);

        assertNotNull(resposta);
        assertEquals(StatusConfirmacaoBarcode.CONFIRMADO.toString(), resposta.getStatusConfirmacao());
        assertEquals("CONT123456", resposta.getCodigoBarcode());
        assertTrue(resposta.getMensagem().contains("sucesso"));

        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.CONFIRMADO &&
                gp.getStatus() == StatusGate.LIBERADO &&
                "CONT123456".equals(gp.getCodigoBarcode())
        ));
    }

    @Test
    void deveRejeitarBarcodeComMotivo() {
        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setCodigoBarcode("CONT-INCORRETO");
        request.setConfirmado(false);
        request.setMotivo("Barcode não corresponde ao esperado");
        request.setDispositivoDmtId("DMT-001");

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any(GateEvent.class))).thenReturn(new GateEvent());

        ConfirmacaoBarcodeResponse resposta = service.confirmarBarcode(request);

        assertNotNull(resposta);
        assertEquals(StatusConfirmacaoBarcode.REJEITADO.toString(), resposta.getStatusConfirmacao());
        assertTrue(resposta.getMensagem().contains("rejeitado"));

        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.REJEITADO &&
                gp.getStatus() == StatusGate.RETIDO &&
                "Barcode não corresponde ao esperado".equals(gp.getMotivoRejeicaoBarcode())
        ));
    }

    @Test
    void deveLancarNotFoundExceptionParaTokenInvalido() {
        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-invalido");

        when(gatePassRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.confirmarBarcode(request));
        verify(gatePassRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionParaGatePassFinalizado() {
        gatePass.setStatus(StatusGate.FINALIZADO);

        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setConfirmado(true);

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));

        assertThrows(BusinessException.class, () -> service.confirmarBarcode(request));
        verify(gatePassRepository, never()).save(any());
    }

    @Test
    void deveLancarBusinessExceptionSeJaFoiConfirmado() {
        gatePass.setStatusConfirmacaoBarcode(StatusConfirmacaoBarcode.CONFIRMADO);

        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setConfirmado(true);

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));

        assertThrows(BusinessException.class, () -> service.confirmarBarcode(request));
        verify(gatePassRepository, never()).save(any());
    }

    @Test
    void deveRegistrarTimeoutBarcode() {
        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any(GateEvent.class))).thenReturn(new GateEvent());

        ConfirmacaoBarcodeResponse resposta = service.registrarTimeoutBarcode("token-xyz", "DMT-001");

        assertNotNull(resposta);
        assertEquals(StatusConfirmacaoBarcode.TIMEOUT.toString(), resposta.getStatusConfirmacao());
        assertTrue(resposta.getMensagem().contains("Timeout"));

        verify(gatePassRepository).save(argThat(gp ->
                gp.getStatusConfirmacaoBarcode() == StatusConfirmacaoBarcode.TIMEOUT &&
                gp.getStatus() == StatusGate.RETIDO
        ));
    }

    @Test
    void deveUsarTimestampProvidoNaConfirmacao() {
        LocalDateTime timestampEsperado = LocalDateTime.of(2026, 6, 2, 15, 30, 0);

        ConfirmacaoBarcodeRequest request = new ConfirmacaoBarcodeRequest();
        request.setTokenGatePass("token-xyz");
        request.setCodigoBarcode("CONT123456");
        request.setConfirmado(true);
        request.setDataConfirmacao(timestampEsperado);
        request.setDispositivoDmtId("DMT-001");

        when(gatePassRepository.findByToken("token-xyz")).thenReturn(Optional.of(gatePass));
        when(gatePassRepository.save(any(GatePass.class))).thenReturn(gatePass);
        when(gateEventRepository.save(any(GateEvent.class))).thenReturn(new GateEvent());

        ConfirmacaoBarcodeResponse resposta = service.confirmarBarcode(request);

        assertEquals(timestampEsperado, resposta.getDataConfirmacao());
    }
}
