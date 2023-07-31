package br.com.cloudport.servicoautenticacao.dto;


import java.util.Set;

public class LoginResponseDTO {
    private final String token;
    private final Set<String> roles;

    public LoginResponseDTO(String token, Set<String> roles) {
        this.token = token;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Set<String> getRoles() {
        return roles;
    }
}

