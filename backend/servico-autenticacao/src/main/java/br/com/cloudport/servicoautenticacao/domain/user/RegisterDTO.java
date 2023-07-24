package br.com.cloudport.servicoautenticacao.domain.user;

public record RegisterDTO(String login, String password, UserRole role) {
}
