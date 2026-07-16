package br.com.cloudport.servicoautenticacao.app.administracao;

import br.com.cloudport.servicoautenticacao.app.administracao.dto.UsuarioInfoDTO;
import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConsultaUsuarioServico {

    private final UsuarioRepositorio usuarioRepositorio;

    public ConsultaUsuarioServico(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioInfoDTO> buscarPorLogin(String login) {
        if (!StringUtils.hasText(login)) {
            return Optional.empty();
        }
        return usuarioRepositorio.findByLogin(login.trim())
                .map(UsuarioInfoDTO::fromUsuario);
    }
}
