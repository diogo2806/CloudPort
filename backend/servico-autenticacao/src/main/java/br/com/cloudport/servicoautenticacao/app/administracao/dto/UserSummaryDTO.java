package br.com.cloudport.servicoautenticacao.app.administracao.dto;

import br.com.cloudport.servicoautenticacao.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryDTO {
    private final UUID id;
    private final String nome;
    private final String email;
    private final String status;

    public UserSummaryDTO(UUID id, String nome, String email, String status) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public static UserSummaryDTO fromUser(User user) {
        String nome = user.getNome() != null && !user.getNome().isBlank() ? user.getNome() : user.getLogin();
        String email = user.getLogin();
        String status = user.isEnabled() ? "Ativo" : "Inativo";
        return new UserSummaryDTO(user.getId(), nome, email, status);
    }
}
