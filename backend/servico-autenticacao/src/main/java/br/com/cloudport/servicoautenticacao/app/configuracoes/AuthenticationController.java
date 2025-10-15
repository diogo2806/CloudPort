package br.com.cloudport.servicoautenticacao.app.configuracoes;

import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.LoginResponseDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.RegisterDTO;
import br.com.cloudport.servicoautenticacao.app.administracao.dto.UserInfoDTO;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.app.administracao.RoleRepository;
import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.app.administracao.UserRepository;
import br.com.cloudport.servicoautenticacao.app.administracao.UserRoleRepository;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Value("${api.security.self-registration.allowed-roles:USER}")
    private String allowedSelfRegistrationRoles;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.getLogin(), data.getPassword());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User user = (User) auth.getPrincipal();
        var token = tokenService.generateToken(user);

        Set<String> roles = java.util.Optional.ofNullable(user.getRoles()).orElse(java.util.Collections.emptySet()).stream()
                                 .map(UserRole::getRole)
                                 .map(Role::getName)
                                 .filter(StringUtils::hasText)
                                 .map(roleName -> roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName.toUpperCase())
                                 .collect(Collectors.toCollection(LinkedHashSet::new));

        String perfil = roles.stream().findFirst().orElse("");
        String nome = StringUtils.hasText(user.getNome()) ? user.getNome() : user.getLogin();

        return ResponseEntity.ok(new LoginResponseDTO(
                user.getId(),
                user.getLogin(),
                nome,
                perfil,
                token,
                roles,
                user.getTransportadoraDocumento(),
                user.getTransportadoraNome()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data){
        if (this.userRepository.findByLogin(data.getLogin()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login informado já está em uso.");
        }
    
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());

        Set<String> normalizedAllowedRoles = Arrays.stream(allowedSelfRegistrationRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), set -> set.isEmpty() ? Collections.singleton("USER") : set));

        Set<String> requestedRoles = data.getRoles().stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toSet());

        if (!normalizedAllowedRoles.containsAll(requestedRoles)) {
            // SECURITY: impede elevação de privilégio restringindo as roles permitidas no auto-registro
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles informadas não são permitidas para auto-registro.");
        }

        Set<UserRole> roles = requestedRoles.stream()
                              .map(roleName -> {
                                  Role role = roleRepository.findByName(roleName)
                                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role '" + roleName + "' não encontrada."));
                                  return new UserRole(null, role);
                              })
                              .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            // SECURITY: garante que ao menos uma role segura seja atribuída ao usuário
            roles = normalizedAllowedRoles.stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .map(role -> new UserRole(null, role))
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role '" + roleName + "' não encontrada.")))
                    .collect(Collectors.toSet());
        }

        User newUser = new User(data.getLogin(), encryptedPassword, data.getNome(),
                data.getTransportadoraDocumento(), data.getTransportadoraNome(), roles);

        roles.forEach(role -> role.setUser(newUser));

        this.userRepository.save(newUser);
        this.userRoleRepository.saveAll(roles);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/usuarios/{login}")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<UserInfoDTO> buscarUsuario(@PathVariable String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return ResponseEntity.ok(UserInfoDTO.fromUser(user));
    }
}
