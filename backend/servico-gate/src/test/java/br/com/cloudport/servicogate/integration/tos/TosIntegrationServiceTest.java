package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.dto.TosContainerStatus;
import br.com.cloudport.servicogate.integration.tos.model.TosBookingResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosContainerStatusResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import br.com.cloudport.servicogate.model.Agendamento;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class TosIntegrationServiceTest {

    private TosClient tosClient;
    private TosResponseAdapter adapter;
    private CacheManager cacheManager;
    private TosIntegrationService service;

    @BeforeEach
    void setUp() {
        tosClient = Mockito.mock(TosClient.class);
        adapter = new TosResponseAdapter();
        cacheManager = new ConcurrentMapCacheManager(
                TosCacheNames.BOOKING,
                TosCacheNames.CONTAINER_STATUS,
                TosCacheNames.CUSTOMS_RELEASE);
        service = new TosIntegrationService(tosClient, adapter, cacheManager);
    }

    @Test
    void deveLancarExcecaoQuandoBookingNaoEhEncontrado() {
        when(tosClient.buscarBooking("BK404")).thenReturn(null);

        TosIntegrationException exception = assertThrows(TosIntegrationException.class,
                () -> service.validarAgendamentoParaCriacao("BK404", TipoOperacao.ENTRADA));

        assertThat(exception.getMessage()).contains("Booking BK404 não localizado");
    }

    @Test
    void deveLancarExcecaoQuandoBookingNaoEstaLiberado() {
        TosBookingResponse response = new TosBookingResponse();
        response.setBookingNumber("BK001");
        response.setReleased(false);
        response.setDenialReason("Bloqueio documental");

        when(tosClient.buscarBooking("BK001")).thenReturn(response);

        TosIntegrationException exception = assertThrows(TosIntegrationException.class,
                () -> service.validarAgendamentoParaCriacao("BK001", TipoOperacao.ENTRADA));

        assertThat(exception.getMessage()).contains("Bloqueio documental");
    }

    @Test
    void deveRetornarStatusQuandoGateELiberacaoAduaneiraPermitidos() {
        TosContainerStatusResponse statusResponse = new TosContainerStatusResponse();
        statusResponse.setContainerNumber("CONT1");
        statusResponse.setStatus("LIBERADO");
        statusResponse.setGateAllowed(true);
        statusResponse.setLastUpdate(OffsetDateTime.now());

        TosCustomsReleaseResponse customsResponse = new TosCustomsReleaseResponse();
        customsResponse.setReleased(true);

        when(tosClient.buscarStatusContainer("CONT1")).thenReturn(statusResponse);
        when(tosClient.buscarLiberacaoAduaneira("CONT1")).thenReturn(customsResponse);

        Agendamento agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setCodigo("CONT1");

        TosContainerStatus status = service.validarParaEntrada(agendamento);

        assertThat(status).isNotNull();
        assertThat(status.isGateLiberado()).isTrue();
        assertThat(status.isLiberacaoAduaneira()).isTrue();
    }

    @Test
    void deveLancarExcecaoQuandoGateNaoLiberado() {
        TosContainerStatusResponse statusResponse = new TosContainerStatusResponse();
        statusResponse.setContainerNumber("CONT2");
        statusResponse.setStatus("HOLD");
        statusResponse.setGateAllowed(false);
        statusResponse.setHoldReason("Aguardando inspeção");

        TosCustomsReleaseResponse customsResponse = new TosCustomsReleaseResponse();
        customsResponse.setReleased(true);

        when(tosClient.buscarStatusContainer("CONT2")).thenReturn(statusResponse);
        when(tosClient.buscarLiberacaoAduaneira("CONT2")).thenReturn(customsResponse);

        Agendamento agendamento = new Agendamento();
        agendamento.setCodigo("CONT2");

        TosIntegrationException exception = assertThrows(TosIntegrationException.class,
                () -> service.validarParaEntrada(agendamento));

        assertThat(exception.getMessage()).contains("TOS bloqueou o gate");
    }

    @Test
    void deveLancarExcecaoQuandoLiberacaoAduaneiraNaoPermitida() {
        TosContainerStatusResponse statusResponse = new TosContainerStatusResponse();
        statusResponse.setContainerNumber("CONT3");
        statusResponse.setStatus("CUSTOMS_HOLD");
        statusResponse.setGateAllowed(true);
        statusResponse.setHoldReason("Restrição aduaneira");

        TosCustomsReleaseResponse customsResponse = new TosCustomsReleaseResponse();
        customsResponse.setReleased(false);
        customsResponse.setDenialReason("Falha documental");

        when(tosClient.buscarStatusContainer("CONT3")).thenReturn(statusResponse);
        when(tosClient.buscarLiberacaoAduaneira("CONT3")).thenReturn(customsResponse);

        Agendamento agendamento = new Agendamento();
        agendamento.setCodigo("CONT3");

        TosIntegrationException exception = assertThrows(TosIntegrationException.class,
                () -> service.validarParaEntrada(agendamento));

        assertThat(exception.getMessage()).contains("pendência aduaneira");
        assertThat(exception.getMessage()).contains("Falha documental");
    }
}
