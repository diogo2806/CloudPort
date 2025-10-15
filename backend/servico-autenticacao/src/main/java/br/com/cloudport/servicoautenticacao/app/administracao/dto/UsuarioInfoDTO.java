package br.com.cloudport.servicoautenticacao.app.administracao.dto;

import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class UsuarioInfoDTO {

    private final UUID id;
    private final String login;
    private final String nome;
    private final Set<String> papeis;
    private final String perfil;
    private final String transportadoraDocumento;
    private final String transportadoraNome;

    public UsuarioInfoDTO(UUID id,
                          String login,
                          String nome,
                          Set<String> papeis,
                          String perfil,
                          String transportadoraDocumento,
                          String transportadoraNome) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.papeis = papeis;
        this.perfil = perfil;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
    }

    public static UsuarioInfoDTO fromUsuario(Usuario usuario) {
        Set<String> papeis = usuario.getPapeis().stream()
                .map(UsuarioPapel::getPapel)
                .map(Papel::getNome)
                .filter(StringUtils::hasText)
                .map(nomePapel -> nomePapel.startsWith("ROLE_") ? nomePapel : "ROLE_" + nomePapel.toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String perfil = papeis.stream().findFirst().orElse("");
        String nome = StringUtils.hasText(usuario.getNome()) ? usuario.getNome() : usuario.getLogin();
        return new UsuarioInfoDTO(
                usuario.getId(),
                usuario.getLogin(),
                nome,
                papeis,
                perfil,
                usuario.getTransportadoraDocumento(),
                usuario.getTransportadoraNome()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public Set<String> getPapeis() {
        return papeis;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getTransportadoraDocumento() {
        return transportadoraDocumento;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }
}
