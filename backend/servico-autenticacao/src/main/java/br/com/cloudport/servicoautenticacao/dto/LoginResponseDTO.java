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
    private final String transportadoraDocumento;
    private final String transportadoraNome;

    public LoginResponseDTO(UUID id,
                            String login,
                            String nome,
                            String perfil,
                            String token,
                            Set<String> roles,
                            String transportadoraDocumento,
                            String transportadoraNome) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.perfil = perfil;
        this.token = token;
        this.roles = roles;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
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

    public String getTransportadoraDocumento() {
        return transportadoraDocumento;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }
}

