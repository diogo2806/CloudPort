package br.com.cloudport.servicoautenticacao.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Procuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeAssociacao;
    private String cnpjAssociacao;
    private String referenciaProcuracao;
    private LocalDate dataEmissaoProcuracao;
    private LocalDate dataValidadeProcuracao;

    @ManyToOne
    private Usuario usuario;
}
