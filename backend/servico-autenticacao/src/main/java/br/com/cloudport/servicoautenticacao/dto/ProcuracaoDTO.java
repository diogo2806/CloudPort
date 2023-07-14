package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProcuracaoDTO {

    private Long id;
    private String referenciaProcuracao;
    private LocalDate dataEmissao;
    private LocalDate dataValidade;
    private String nomeUsuario;

    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
