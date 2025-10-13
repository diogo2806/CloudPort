package br.com.cloudport.servicogate.security;

import java.util.Set;

public class UserInfoResponse {

    private String id;
    private String login;
    private String nome;
    private String perfil;
    private Set<String> roles;
    private String transportadoraDocumento;
    private String transportadoraNome;

    public UserInfoResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getTransportadoraDocumento() {
        return transportadoraDocumento;
    }

    public void setTransportadoraDocumento(String transportadoraDocumento) {
        this.transportadoraDocumento = transportadoraDocumento;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }
}
