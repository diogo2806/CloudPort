package br.com.cloudport.servicoautenticacao.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

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
    
    @Enumerated(EnumType.STRING)
    private StatusCadastroEnum statusCadastroEnum;

    private String usuarioAprovacao;
    private LocalDate dataAprovacao;
    private LocalDate dataCadastro;

    @Enumerated(EnumType.STRING)
    private TipoUsuarioEnum tipoUsuarioEnum;  

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "usuario_perfil_acesso",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "perfil_acesso_id"))
    private List<PerfilAcesso> perfisAcesso;

    @OneToMany(mappedBy = "usuario")
    private List<Procuracao> procuracoes;
    
    // Construtor com todos os atributos (você pode escolher quais atributos incluir no construtor)
    public Usuario(String cpf, String nome, String email, String cnpj, String razaoSocial, 
        StatusCadastroEnum statusCadastro, String usuarioAprovacao, LocalDate dataAprovacao,
        LocalDate dataCadastro) {
        this.cpf = cpf;
        this.nome = nome;
        this.email = email;
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.statusCadastroEnum = statusCadastro;
        this.usuarioAprovacao = usuarioAprovacao;
        this.dataAprovacao = dataAprovacao;
        this.dataCadastro = dataCadastro;
    }
}
