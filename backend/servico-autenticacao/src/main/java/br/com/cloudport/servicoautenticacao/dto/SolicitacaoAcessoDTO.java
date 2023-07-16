package br.com.cloudport.servicoautenticacao.dto;

import br.com.cloudport.servicoautenticacao.model.SolicitacaoAcesso;
import br.com.cloudport.servicoautenticacao.model.StatusCadastro;
import java.time.LocalDate;

public class SolicitacaoAcessoDTO {

    private Long id;
    private UsuarioDTO usuario;
    private StatusCadastro status;
    private LocalDate dataSolicitacao;
    private String justificativa;

    public SolicitacaoAcessoDTO() {
    }

    public SolicitacaoAcessoDTO(SolicitacaoAcesso solicitacaoAcesso) {
        this.id = solicitacaoAcesso.getId();
        this.usuario = new UsuarioDTO(solicitacaoAcesso.getUsuario());
        this.status = solicitacaoAcesso.getStatus();
        this.dataSolicitacao = solicitacaoAcesso.getDataSolicitacao();
        this.justificativa = solicitacaoAcesso.getJustificativa();
    }

    public SolicitacaoAcesso toModel() {
        SolicitacaoAcesso solicitacaoAcesso = new SolicitacaoAcesso();
        solicitacaoAcesso.setId(id);
        solicitacaoAcesso.setUsuario(usuario.toModel());
        solicitacaoAcesso.setStatus(status);
        solicitacaoAcesso.setDataSolicitacao(dataSolicitacao);
        solicitacaoAcesso.setJustificativa(justificativa);
        return solicitacaoAcesso;
    }

    // getters and setters...
}
