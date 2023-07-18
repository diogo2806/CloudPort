package br.com.cloudport.servicoautenticacao.model;

import java.time.LocalDate;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razaoSocial;
    private String cnpj;
    private String nomeFantasia;
    private String status;
    private LocalDate dataCriacao;
    private LocalDate dataAlteracao;
    private boolean estrangeiro;
    
    //Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String pais;
    private String municipio;

    //Ramos de Atividade
    private boolean importador;
    private boolean exportador;
    private boolean despachante;
    private boolean transportadora;
    private boolean cliente;
    private boolean fornecedor;
    private boolean prestadorDeServico;
    private boolean operadorPortuario;
    private boolean armador;
    private boolean agencia;
    private boolean parceiro;
    private boolean depositante;
    private boolean arrematante;
}
