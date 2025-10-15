package br.com.cloudport.servicoautenticacao.app.usuarioslista.dto;

import br.com.cloudport.servicoautenticacao.model.Usuario;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsuarioResumoDTO {
    private final UUID id;
    private final String nome;
    private final String email;
    private final String status;

    public UsuarioResumoDTO(UUID id, String nome, String email, String status) {
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

    public static UsuarioResumoDTO fromUsuario(Usuario usuario) {
        String nome = usuario.getNome() != null && !usuario.getNome().isBlank() ? usuario.getNome() : usuario.getLogin();
        String email = usuario.getLogin();
        String status = usuario.isEnabled() ? "Ativo" : "Inativo";
        return new UsuarioResumoDTO(usuario.getId(), nome, email, status);
    }
}
