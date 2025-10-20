package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

import java.util.ArrayList;
import java.util.List;

public class CentralAcaoAgendamentoRespostaDTO {

    private UsuarioCentralAcaoDTO usuario;
    private SituacaoPatioDTO situacaoPatio;
    private List<VisaoCompletaAgendamentoDTO> agendamentos = new ArrayList<>();

    public CentralAcaoAgendamentoRespostaDTO() {
    }

    public CentralAcaoAgendamentoRespostaDTO(UsuarioCentralAcaoDTO usuario,
                                             SituacaoPatioDTO situacaoPatio,
                                             List<VisaoCompletaAgendamentoDTO> agendamentos) {
        this.usuario = usuario;
        this.situacaoPatio = situacaoPatio;
        if (agendamentos != null) {
            this.agendamentos = agendamentos;
        }
    }

    public UsuarioCentralAcaoDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioCentralAcaoDTO usuario) {
        this.usuario = usuario;
    }

    public SituacaoPatioDTO getSituacaoPatio() {
        return situacaoPatio;
    }

    public void setSituacaoPatio(SituacaoPatioDTO situacaoPatio) {
        this.situacaoPatio = situacaoPatio;
    }

    public List<VisaoCompletaAgendamentoDTO> getAgendamentos() {
        return agendamentos;
    }

    public void setAgendamentos(List<VisaoCompletaAgendamentoDTO> agendamentos) {
        this.agendamentos = agendamentos != null ? agendamentos : new ArrayList<>();
    }
}
