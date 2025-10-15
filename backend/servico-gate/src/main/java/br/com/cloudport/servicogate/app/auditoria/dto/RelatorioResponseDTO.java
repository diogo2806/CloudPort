package br.com.cloudport.servicogate.app.auditoria.dto;

import br.com.cloudport.servicogate.app.transparencia.dto.DashboardResumoDTO;
import java.util.List;

public class RelatorioResponseDTO {

    private DashboardResumoDTO resumo;
    private List<RelatorioAgendamentoDTO> agendamentos;

    public RelatorioResponseDTO() {
    }

    public RelatorioResponseDTO(DashboardResumoDTO resumo, List<RelatorioAgendamentoDTO> agendamentos) {
        this.resumo = resumo;
        this.agendamentos = agendamentos;
    }

    public DashboardResumoDTO getResumo() {
        return resumo;
    }

    public void setResumo(DashboardResumoDTO resumo) {
        this.resumo = resumo;
    }

    public List<RelatorioAgendamentoDTO> getAgendamentos() {
        return agendamentos;
    }

    public void setAgendamentos(List<RelatorioAgendamentoDTO> agendamentos) {
        this.agendamentos = agendamentos;
    }
}
