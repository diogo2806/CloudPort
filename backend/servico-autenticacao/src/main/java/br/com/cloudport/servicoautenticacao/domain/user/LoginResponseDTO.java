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
