package br.com.cloudport.servicoautenticacao.domain.user;


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

/*
package br.com.cloudport.servicoautenticacao.domain.user;

public class LoginResponseDTO {
    private final String token;
    private final String role;

    public LoginResponseDTO(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }
}
*/