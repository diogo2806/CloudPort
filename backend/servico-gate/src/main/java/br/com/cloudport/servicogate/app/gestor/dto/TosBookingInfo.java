package br.com.cloudport.servicogate.app.gestor.dto;

import java.time.LocalDateTime;

public class TosBookingInfo {

    private final String bookingNumber;
    private final String vessel;
    private final String voyage;
    private final LocalDateTime cutoff;
    private final boolean liberado;
    private final String motivoRestricao;

    public TosBookingInfo(String bookingNumber,
                          String vessel,
                          String voyage,
                          LocalDateTime cutoff,
                          boolean liberado,
                          String motivoRestricao) {
        this.bookingNumber = bookingNumber;
        this.vessel = vessel;
        this.voyage = voyage;
        this.cutoff = cutoff;
        this.liberado = liberado;
        this.motivoRestricao = motivoRestricao;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public String getVessel() {
        return vessel;
    }

    public String getVoyage() {
        return voyage;
    }

    public LocalDateTime getCutoff() {
        return cutoff;
    }

    public boolean isLiberado() {
        return liberado;
    }

    public String getMotivoRestricao() {
        return motivoRestricao;
    }
}
