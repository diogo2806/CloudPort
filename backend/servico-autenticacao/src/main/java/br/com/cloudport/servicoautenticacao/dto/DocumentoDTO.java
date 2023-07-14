package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentoDTO {

    private Long id;
    private String nome;
    private String tipo;
    private String conteudo;  // Se o documento for armazenado como uma string

    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
