package br.com.cloudport.servicoautenticacao.dto;


import java.util.Set;

public class RegisterDTO {
    private String login;
    private String password;
    private Set<String> roles;

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
