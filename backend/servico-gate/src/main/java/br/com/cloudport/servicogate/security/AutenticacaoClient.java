package br.com.cloudport.servicogate.security;

import java.util.Optional;

/** Porta do Gate para consulta de identidade e perfis. */
public interface AutenticacaoClient {

    Optional<UserInfoResponse> buscarUsuario(String login, String authorizationHeader);
}
