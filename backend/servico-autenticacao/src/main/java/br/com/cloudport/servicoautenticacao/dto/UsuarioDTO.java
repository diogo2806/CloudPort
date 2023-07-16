package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

import br.com.cloudport.servicoautenticacao.model.Usuario;

@Getter
@Setter
public class UsuarioDTO {

    public UsuarioDTO(Long id2, Object nome2) {
    }
    public UsuarioDTO(Usuario usuario) {
    }
    private Long id;
    private String cpf;
    private String nome;
    private String email;
    private String cnpj;
    private String razaoSocial;
    private String statusCadastro;
    private String usuarioAprovacao;
    private LocalDate dataAprovacao;
    private LocalDate dataCadastro;
    private String tipoUsuario;  
    private List<PerfilAcessoDTO> perfisAcesso;
    private List<ProcuracaoDTO> procuracoes;

    public Usuario toModel() {
        Usuario usuario = new Usuario();
        usuario.setId(this.id);
        usuario.setNome(this.nome);
        // Adicione todas as outras propriedades do usuário aqui...
        return usuario;
    }
    
    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
