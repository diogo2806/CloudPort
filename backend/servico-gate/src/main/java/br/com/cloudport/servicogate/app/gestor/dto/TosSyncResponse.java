package br.com.cloudport.servicogate.app.gestor.dto;

public class TosSyncResponse {

    private final Long agendamentoId;
    private final TosBookingInfo booking;
    private final TosContainerStatus containerStatus;

    public TosSyncResponse(Long agendamentoId, TosBookingInfo booking, TosContainerStatus containerStatus) {
        this.agendamentoId = agendamentoId;
        this.booking = booking;
        this.containerStatus = containerStatus;
    }

    public Long getAgendamentoId() {
        return agendamentoId;
    }

    public TosBookingInfo getBooking() {
        return booking;
    }

    public TosContainerStatus getContainerStatus() {
        return containerStatus;
    }
}
