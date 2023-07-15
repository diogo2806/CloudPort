package br.com.cloudport.servicoautenticacao.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class SolicitacaoAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private StatusCadastro status;

    private LocalDate dataSolicitacao;

    @Column(length = 500)
    private String justificativa;

    // Construtores, getters e setters
}
