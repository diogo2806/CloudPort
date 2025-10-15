package br.com.cloudport.servicoautenticacao.app.administracao.dto;

import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public class UserInfoDTO {

    private final UUID id;
    private final String login;
    private final String nome;
    private final Set<String> roles;
    private final String perfil;
    private final String transportadoraDocumento;
    private final String transportadoraNome;

    public UserInfoDTO(UUID id,
                       String login,
                       String nome,
                       Set<String> roles,
                       String perfil,
                       String transportadoraDocumento,
                       String transportadoraNome) {
        this.id = id;
        this.login = login;
        this.nome = nome;
        this.roles = roles;
        this.perfil = perfil;
        this.transportadoraDocumento = transportadoraDocumento;
        this.transportadoraNome = transportadoraNome;
    }

    public static UserInfoDTO fromUser(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(roleName -> roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName.toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String perfil = roles.stream().findFirst().orElse("");
        String nome = StringUtils.hasText(user.getNome()) ? user.getNome() : user.getLogin();
        return new UserInfoDTO(
                user.getId(),
                user.getLogin(),
                nome,
                roles,
                perfil,
                user.getTransportadoraDocumento(),
                user.getTransportadoraNome()
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

    public Set<String> getRoles() {
        return roles;
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
