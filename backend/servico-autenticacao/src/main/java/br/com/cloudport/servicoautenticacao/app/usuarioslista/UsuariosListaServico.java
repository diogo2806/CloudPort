package br.com.cloudport.servicoautenticacao.app.usuarioslista;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.dto.UsuarioResumoDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UsuariosListaServico {

    private final UsuarioRepositorio usuarioRepositorio;

    public UsuariosListaServico(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    public List<UsuarioResumoDTO> listarUsuariosResumo() {
        return usuarioRepositorio.findAll()
                .stream()
                .map(UsuarioResumoDTO::fromUsuario)
                .sorted((usuarioA, usuarioB) -> usuarioA.getNome().compareToIgnoreCase(usuarioB.getNome()))
                .collect(Collectors.toList());
    }
}
