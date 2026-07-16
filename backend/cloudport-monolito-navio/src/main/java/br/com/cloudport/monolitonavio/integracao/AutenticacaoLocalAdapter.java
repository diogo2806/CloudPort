package br.com.cloudport.monolitonavio.integracao;

import br.com.cloudport.servicoautenticacao.app.administracao.ConsultaUsuarioServico;
import br.com.cloudport.servicoautenticacao.app.administracao.dto.UsuarioInfoDTO;
import br.com.cloudport.servicogate.security.AutenticacaoClient;
import br.com.cloudport.servicogate.security.UserInfoResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cloudport.modulo.autenticacao.integracao", havingValue = "local")
public class AutenticacaoLocalAdapter implements AutenticacaoClient {

    private final ConsultaUsuarioServico consultaUsuarioServico;

    public AutenticacaoLocalAdapter(ConsultaUsuarioServico consultaUsuarioServico) {
        this.consultaUsuarioServico = consultaUsuarioServico;
    }

    @Override
    public Optional<UserInfoResponse> buscarUsuario(String login, String authorizationHeader) {
        return consultaUsuarioServico.buscarPorLogin(login)
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
