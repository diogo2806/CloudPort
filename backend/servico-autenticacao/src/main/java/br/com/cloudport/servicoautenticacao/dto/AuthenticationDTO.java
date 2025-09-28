package br.com.cloudport.servicoautenticacao.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AuthenticationDTO {
    @NotBlank(message = "O login é obrigatório.")
    @Size(min = 3, max = 100, message = "O login deve ter entre 3 e 100 caracteres.")
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 255, message = "A senha deve ter pelo menos 6 caracteres.")
    private String password;

    public AuthenticationDTO() {}

    public AuthenticationDTO(String login, String password) {
        this.login = login;
        this.password = password;
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
}
