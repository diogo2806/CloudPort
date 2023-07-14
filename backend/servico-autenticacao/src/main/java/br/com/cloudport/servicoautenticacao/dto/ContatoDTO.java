package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContatoDTO {
    
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    
    // Você pode adicionar qualquer outro campo necessário de acordo com sua lógica de negócio

    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
