package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.dto.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.dto.LoginResponseDTO;
import br.com.cloudport.servicoautenticacao.dto.RegisterDTO;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import br.com.cloudport.servicoautenticacao.repositories.UserRoleRepository;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

        var token = tokenService.generateToken((User) auth.getPrincipal());
        
        User user = (User) auth.getPrincipal();
        Set<String> roles = user.getRoles().stream()
                                 .map(userRole -> userRole.getRole().getName())
                                 .collect(Collectors.toSet());

        return ResponseEntity.ok(new LoginResponseDTO(token, roles));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data){
        if(this.userRepository.findByLogin(data.getLogin()).isPresent()) return ResponseEntity.badRequest().build();
    
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Roles informadas não são permitidas para auto-registro");
        }

        Set<UserRole> roles = requestedRoles.stream()
                              .map(roleName -> {
                                  Role role = roleRepository.findByName(roleName)
                                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + roleName + " not found"));
                                  return new UserRole(null, role);
                              })
                              .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            // SECURITY: garante que ao menos uma role segura seja atribuída ao usuário
            roles = normalizedAllowedRoles.stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .map(role -> new UserRole(null, role))
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + roleName + " not found")))
                    .collect(Collectors.toSet());
        }

        User newUser = new User(data.getLogin(), encryptedPassword, roles);

        roles.forEach(role -> role.setUser(newUser));

        this.userRepository.save(newUser);
        this.userRoleRepository.saveAll(roles);

        return ResponseEntity.ok().build();
    }
}
