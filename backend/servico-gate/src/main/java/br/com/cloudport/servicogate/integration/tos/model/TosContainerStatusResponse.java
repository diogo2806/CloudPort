package br.com.cloudport.servicogate.integration.tos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public class TosContainerStatusResponse {

    @JsonProperty("containerNumber")
    private String containerNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("gateAllowed")
    private boolean gateAllowed;

    @JsonProperty("holdReason")
    private String holdReason;

    @JsonProperty("lastUpdate")
    private OffsetDateTime lastUpdate;

    public String getContainerNumber() {
        return containerNumber;
    }

    public void setContainerNumber(String containerNumber) {
        this.containerNumber = containerNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isGateAllowed() {
        return gateAllowed;
    }

    public void setGateAllowed(boolean gateAllowed) {
        this.gateAllowed = gateAllowed;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
