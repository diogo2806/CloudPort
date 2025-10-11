package br.com.cloudport.servicogate.integration.tos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TosCustomsReleaseResponse {

    @JsonProperty("containerNumber")
    private String containerNumber;

    @JsonProperty("released")
    private boolean released;

    @JsonProperty("denialReason")
    private String denialReason;

    public String getContainerNumber() {
        return containerNumber;
    }

    public void setContainerNumber(String containerNumber) {
        this.containerNumber = containerNumber;
    }

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }
}
