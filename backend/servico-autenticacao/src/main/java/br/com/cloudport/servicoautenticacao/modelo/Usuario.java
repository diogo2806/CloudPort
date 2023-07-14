package br.com.cloudport.servicoautenticacao.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cpf;
    private String nome;
    private String email;
    private String cnpj;
    private String razaoSocial;
    private String perfilAcesso;
    
    @Enumerated(EnumType.STRING)
    private StatusCadastro statusCadastro;

    private String usuarioAprovacao;
    private LocalDate dataAprovacao;
    private LocalDate dataCadastro;

    @Enumerated(EnumType.STRING)
    private TipoUsuario tipoUsuario;  // adicione essa linha
    
    // Construtor com todos os atributos (você pode escolher quais atributos incluir no construtor)
    public Usuario(String cpf, String nome, String email, String cnpj, String razaoSocial, 
        String perfilAcesso, StatusCadastro statusCadastro, String usuarioAprovacao,
        LocalDate dataAprovacao, LocalDate dataCadastro) {
        this.cpf = cpf;
        this.nome = nome;
        this.email = email;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.perfilAcesso = perfilAcesso;
        this.statusCadastro = statusCadastro;
        this.usuarioAprovacao = usuarioAprovacao;
        this.dataAprovacao = dataAprovacao;
        this.dataCadastro = dataCadastro;
    }
}
