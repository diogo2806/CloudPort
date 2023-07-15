package br.com.cloudport.servicoautenticacao.dto;

import br.com.cloudport.servicoautenticacao.model.AcessoSistema;
import br.com.cloudport.servicoautenticacao.model.Usuario;
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

    public SolicitacaoAcessoDTO(AcessoSistema acessoSistema) {
        this.id = acessoSistema.getId();
        this.usuario = new UsuarioDTO(acessoSistema.getUsuario().getId(), acessoSistema.getUsuario().getNome());
        this.status = acessoSistema.getStatus();
        this.dataSolicitacao = acessoSistema.getDataSolicitacao();
        this.justificativa = acessoSistema.getJustificativa();
    }

    public AcessoSistema toModel() {
        AcessoSistema acessoSistema = new AcessoSistema();
        acessoSistema.setId(id);
        acessoSistema.setUsuario(new Usuario(usuario.getId(), usuario.getNome()));
        acessoSistema.setStatus(status);
        acessoSistema.setDataSolicitacao(dataSolicitacao);
        acessoSistema.setJustificativa(justificativa);
        return acessoSistema;
    }

    // getters and setters...
}
