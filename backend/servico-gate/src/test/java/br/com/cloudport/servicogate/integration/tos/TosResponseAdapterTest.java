package br.com.cloudport.servicogate.integration.tos;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.cloudport.servicogate.dto.TosBookingInfo;
import br.com.cloudport.servicogate.dto.TosContainerStatus;
import br.com.cloudport.servicogate.integration.tos.model.TosBookingResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosContainerStatusResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TosResponseAdapterTest {

    private TosResponseAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TosResponseAdapter();
    }

    @Test
    void deveConverterBookingResponseParaInfoInterna() {
        TosBookingResponse response = new TosBookingResponse();
        response.setBookingNumber("BK123");
        response.setReleased(true);
        response.setDenialReason("Nenhuma");
        response.setVessel("Evergreen");
        response.setVoyage("EVG-01");
        response.setCutoff(OffsetDateTime.of(2023, 12, 10, 15, 30, 0, 0, ZoneOffset.UTC));

        TosBookingInfo info = adapter.toBookingInfo(response);

        assertThat(info).isNotNull();
        assertThat(info.getBookingNumber()).isEqualTo("BK123");
        assertThat(info.getVessel()).isEqualTo("Evergreen");
        assertThat(info.getVoyage()).isEqualTo("EVG-01");
        assertThat(info.getCutoff()).isEqualTo(response.getCutoff().toLocalDateTime());
        assertThat(info.isLiberado()).isTrue();
        assertThat(info.getMotivoRestricao()).isEqualTo("Nenhuma");
    }

    @Test
    void deveConverterStatusEIntegrarComLiberacaoAduaneira() {
        TosContainerStatusResponse statusResponse = new TosContainerStatusResponse();
        statusResponse.setContainerNumber("CONT1");
        statusResponse.setStatus("HOLD");
        statusResponse.setGateAllowed(false);
        statusResponse.setHoldReason("Documentos pendentes");
        statusResponse.setLastUpdate(OffsetDateTime.now(ZoneOffset.UTC));

        TosCustomsReleaseResponse customsResponse = new TosCustomsReleaseResponse();
        customsResponse.setReleased(true);
        customsResponse.setDenialReason("Sem restrições");

        TosContainerStatus status = adapter.toContainerStatus(statusResponse, customsResponse);

        assertThat(status).isNotNull();
        assertThat(status.getContainerNumber()).isEqualTo("CONT1");
        assertThat(status.getStatus()).isEqualTo("HOLD");
        assertThat(status.isGateLiberado()).isFalse();
        assertThat(status.isLiberacaoAduaneira()).isTrue();
        assertThat(status.getMotivoRestricao()).isEqualTo("Documentos pendentes");
        assertThat(status.getUltimaAtualizacao()).isEqualTo(statusResponse.getLastUpdate().toLocalDateTime());
    }

    @Test
    void devePreservarMotivoDoTosQuandoLiberacaoAduaneiraNegada() {
        TosContainerStatusResponse statusResponse = new TosContainerStatusResponse();
        statusResponse.setContainerNumber("CONT2");
        statusResponse.setStatus("CUSTOMS_HOLD");
        statusResponse.setGateAllowed(true);
        statusResponse.setHoldReason("Sem motivo informado");
        statusResponse.setLastUpdate(OffsetDateTime.now(ZoneOffset.UTC));

        TosCustomsReleaseResponse customsResponse = new TosCustomsReleaseResponse();
        customsResponse.setReleased(false);
        customsResponse.setDenialReason("Aguardando Receita Federal");

        TosContainerStatus status = adapter.toContainerStatus(statusResponse, customsResponse);

        assertThat(status).isNotNull();
        assertThat(status.isLiberacaoAduaneira()).isFalse();
        assertThat(status.getMotivoRestricao()).isEqualTo("Aguardando Receita Federal");
    }
}
