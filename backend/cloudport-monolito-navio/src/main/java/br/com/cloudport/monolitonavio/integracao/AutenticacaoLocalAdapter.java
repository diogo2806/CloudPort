package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.servicoautenticacao.app.administracao.dto.UsuarioInfoDTO;
import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import br.com.cloudport.servicogate.security.UserInfoResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.autenticacao.integracao", havingValue = "local")
public class AutenticacaoLocalAdapter implements AutenticacaoClient {

    private final UsuarioRepositorio usuarioRepositorio;

    public AutenticacaoLocalAdapter(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public Optional<UserInfoResponse> buscarUsuario(String login, String authorizationHeader) {
        if (!StringUtils.hasText(login)) {
            return Optional.empty();
        }
        return usuarioRepositorio.findByLogin(login.trim())
                .map(UsuarioInfoDTO::fromUsuario)
                .map(this::converter);
    }

    private UserInfoResponse converter(UsuarioInfoDTO origem) {
        UserInfoResponse destino = new UserInfoResponse();
        destino.setId(origem.getId() == null ? null : origem.getId().toString());
        destino.setLogin(origem.getLogin());
        destino.setNome(origem.getNome());
        destino.setPerfil(origem.getPerfil());
        destino.setRoles(origem.getPapeis());
        destino.setTransportadoraDocumento(origem.getTransportadoraDocumento());
        destino.setTransportadoraNome(origem.getTransportadoraNome());
        return destino;
    }
}
