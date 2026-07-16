package br.com.cloudport.runtime.integracao;

import br.com.cloudport.servicoautenticacao.app.administracao.dto.UsuarioInfoDTO;
import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicogate.monitoring.IntegracaoDegradacaoHandler;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import br.com.cloudport.servicogate.security.UserInfoResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(
        name = "cloudport.modulo.autenticacao.integracao",
        havingValue = "local")
public class AutenticacaoLocalAdapter extends AutenticacaoClient {

    private final UsuarioRepositorio usuarioRepositorio;

    public AutenticacaoLocalAdapter(
            RestTemplate restTemplate,
            IntegracaoDegradacaoHandler degradacaoHandler,
            UsuarioRepositorio usuarioRepositorio) {
        super(restTemplate, "http://autenticacao-local.invalid", degradacaoHandler,
                "Consultar o módulo local de autenticação.");
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public Optional<UserInfoResponse> buscarUsuario(String login, String authorizationHeader) {
        if (!StringUtils.hasText(login)) {
            return Optional.empty();
        }
        return usuarioRepositorio.findByLogin(login)
                .map(UsuarioInfoDTO::fromUsuario)
                .map(this::mapear);
    }

    private UserInfoResponse mapear(UsuarioInfoDTO usuario) {
        UserInfoResponse resposta = new UserInfoResponse();
        resposta.setId(usuario.getId() == null ? null : usuario.getId().toString());
        resposta.setLogin(usuario.getLogin());
        resposta.setNome(usuario.getNome());
        resposta.setPerfil(usuario.getPerfil());
        resposta.setRoles(usuario.getPapeis());
        resposta.setTransportadoraDocumento(usuario.getTransportadoraDocumento());
        resposta.setTransportadoraNome(usuario.getTransportadoraNome());
        return resposta;
    }
}
