package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaDTO {

    private Long id;
    private String razaoSocial;
    private String cnpj;
    private String nomeFantasia;
    private String status;
    private String dataCriacao;
    private String dataAlteracao;

    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
