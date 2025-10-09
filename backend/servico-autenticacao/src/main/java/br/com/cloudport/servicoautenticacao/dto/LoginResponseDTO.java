package br.com.cloudport.servicoautenticacao.dto;


import java.util.Set;
import java.util.UUID;

public class LoginResponseDTO {
    private final UUID id;
    private final String login;
    private final String nome;
    private final String perfil;
    private final String token;
    private final Set<String> roles;

    public LoginResponseDTO(UUID id, String login, String nome, String perfil, String token, Set<String> roles) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
        this.token = token;
        this.roles = roles;
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getToken() {
        return token;
    }

    public Set<String> getRoles() {
        return roles;
    }
}

