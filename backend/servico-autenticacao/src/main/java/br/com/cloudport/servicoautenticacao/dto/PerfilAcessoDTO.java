package br.com.cloudport.servicoautenticacao.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PerfilAcessoDTO {

    private Long id;
    private String nomePerfil;
    private List<PrivilegioDTO> privilegios;

    // Dependendo da sua lógica de negócio, você pode precisar de construtores,
    // métodos para transformar um DTO em uma entidade e vice-versa.
    // Aqui, estou assumindo que você está usando Lombok para gerar getters e setters.
}
