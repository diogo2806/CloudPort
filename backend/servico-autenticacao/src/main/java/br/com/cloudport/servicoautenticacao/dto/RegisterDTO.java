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

    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String nome;

    @Size(max = 30, message = "O documento da transportadora deve ter no máximo 30 caracteres.")
    private String transportadoraDocumento;

    @Size(max = 255, message = "O nome da transportadora deve ter no máximo 255 caracteres.")
    private String transportadoraNome;

    public RegisterDTO() {}

    public RegisterDTO(String login, String password, Set<String> roles) {
        this.login = login;
        this.password = password;
        this.roles = roles;
    }

    public RegisterDTO(String login, String password, Set<String> roles, String nome,
                       String transportadoraDocumento, String transportadoraNome) {
        this(login, password, roles);
        this.nome = nome;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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
