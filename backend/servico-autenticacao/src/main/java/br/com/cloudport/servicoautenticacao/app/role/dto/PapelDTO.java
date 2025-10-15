package br.com.cloudport.servicoautenticacao.app.role.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PapelDTO {
    private Long id;
    private String nome;
    private String status;

    public PapelDTO(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }
}
