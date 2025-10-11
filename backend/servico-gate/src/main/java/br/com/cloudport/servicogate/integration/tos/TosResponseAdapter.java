package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.dto.TosBookingInfo;
import br.com.cloudport.servicogate.dto.TosContainerStatus;
import br.com.cloudport.servicogate.integration.tos.model.TosBookingResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosContainerStatusResponse;
import br.com.cloudport.servicogate.integration.tos.model.TosCustomsReleaseResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TosResponseAdapter {

    public TosBookingInfo toBookingInfo(TosBookingResponse response) {
        if (response == null) {
            return null;
        }
        LocalDateTime cutoff = Optional.ofNullable(response.getCutoff())
                .map(offsetDateTime -> offsetDateTime.toLocalDateTime())
                .orElse(null);
        return new TosBookingInfo(
                response.getBookingNumber(),
                response.getVessel(),
                response.getVoyage(),
                cutoff,
                response.isReleased(),
                response.getDenialReason()
        );
    }

    public TosContainerStatus toContainerStatus(TosContainerStatusResponse statusResponse,
                                                TosCustomsReleaseResponse customsResponse) {
        if (statusResponse == null) {
            return null;
        }
        LocalDateTime lastUpdate = Optional.ofNullable(statusResponse.getLastUpdate())
                .map(offsetDateTime -> offsetDateTime.toLocalDateTime())
                .orElse(null);
        boolean customsReleased = customsResponse == null || customsResponse.isReleased();
        String holdReason = Optional.ofNullable(customsResponse)
                .map(TosCustomsReleaseResponse::getDenialReason)
                .filter(reason -> !reason.isBlank())
                .orElse(statusResponse.getHoldReason());
        return new TosContainerStatus(
                statusResponse.getContainerNumber(),
                statusResponse.getStatus(),
                statusResponse.isGateAllowed(),
                customsReleased,
                lastUpdate,
                holdReason
        );
    }
}
