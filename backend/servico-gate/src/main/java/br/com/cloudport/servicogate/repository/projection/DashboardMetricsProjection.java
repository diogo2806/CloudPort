package br.com.cloudport.servicogate.repository.projection;

public interface DashboardMetricsProjection {

    Long getTotalAgendamentos();

    Long getPontuais();

    Long getNoShow();

    Double getTurnaroundMedio();

    Double getOcupacaoSlots();
}
