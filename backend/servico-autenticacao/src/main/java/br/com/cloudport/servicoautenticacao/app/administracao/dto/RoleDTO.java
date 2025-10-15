package br.com.cloudport.servicoautenticacao.app.administracao.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private String status;
    
    public RoleDTO(Long id, String name) {
        this.id = id;
        this.name = name;
        // você pode definir um valor padrão para status aqui, se quiser
    }
    
    // O Lombok já criou os getters e setters com as anotações @Getter e @Setter
}
