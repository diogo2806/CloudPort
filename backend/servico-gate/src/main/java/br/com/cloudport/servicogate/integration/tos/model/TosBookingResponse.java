package br.com.cloudport.servicogate.integration.tos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public class TosBookingResponse {

    @JsonProperty("bookingNumber")
    private String bookingNumber;

    @JsonProperty("released")
    private boolean released;

    @JsonProperty("denialReason")
    private String denialReason;

    @JsonProperty("vessel")
    private String vessel;

    @JsonProperty("voyage")
    private String voyage;

    @JsonProperty("cutoff")
    private OffsetDateTime cutoff;

    public String getBookingNumber() {
        return bookingNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
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

    public String getVessel() {
        return vessel;
    }

    public void setVessel(String vessel) {
        this.vessel = vessel;
    }

    public String getVoyage() {
        return voyage;
    }

    public void setVoyage(String voyage) {
        this.voyage = voyage;
    }

    public OffsetDateTime getCutoff() {
        return cutoff;
    }

    public void setCutoff(OffsetDateTime cutoff) {
        this.cutoff = cutoff;
    }
}
