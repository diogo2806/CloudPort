package br.com.cloudport.servicogate.app.transparencia;

public interface DashboardMetricsProjection {

    Long getTotalAgendamentos();

    Long getPontuais();

    Long getNoShow();

    Double getTurnaroundMedio();

    Double getOcupacaoSlots();
}
