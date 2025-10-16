package br.com.cloudport.servicoautenticacao.app.configuracoes.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class AuthenticationDTO {
    @NotBlank(message = "O login é obrigatório.")
    @Size(min = 3, max = 100, message = "O login deve ter entre 3 e 100 caracteres.")
    @Pattern(regexp = "^[\\p{L}\\p{N}@._-]+$", message = "O login contém caracteres inválidos.")
    private String login;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 255, message = "A senha deve ter pelo menos 6 caracteres.")
    @JsonProperty("senha")
    @JsonAlias({"password"})
    @Pattern(regexp = "^(?!.*[<>]).*$", message = "A senha não pode conter os caracteres < ou >.")
    private String senha;

    public AuthenticationDTO() {}

    public AuthenticationDTO(String login, String senha) {
        this.login = login;
        this.senha = senha;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
