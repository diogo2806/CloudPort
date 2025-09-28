package br.com.cloudport.servicoautenticacao.dto;


import java.util.Set;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

public class RegisterDTO {
    @NotBlank(message = "O login é obrigatório.")
    @Size(min = 3, max = 100, message = "O login deve ter entre 3 e 100 caracteres.")
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 255, message = "A senha deve ter pelo menos 6 caracteres.")
    private String password;

    @NotEmpty(message = "Informe ao menos uma role.")
    private Set<@NotBlank(message = "A role não pode ser vazia.") String> roles;

    public RegisterDTO() {}

    public RegisterDTO(String login, String password, Set<String> roles) {
        this.login = login;
        this.password = password;
        this.roles = roles;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
