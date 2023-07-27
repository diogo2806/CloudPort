package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.domain.user.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.domain.user.LoginResponseDTO;
import br.com.cloudport.servicoautenticacao.domain.user.RegisterDTO;
import br.com.cloudport.servicoautenticacao.domain.user.User;
import br.com.cloudport.servicoautenticacao.domain.user.Role;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
import br.com.cloudport.servicoautenticacao.infra.security.TokenService;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.getLogin(), data.getPassword());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        
        User user = (User) auth.getPrincipal();
        Set<String> roles = user.getRoles().stream()
                                 .map(Role::getName)
                                 .collect(Collectors.toSet()); 

        return ResponseEntity.ok(new LoginResponseDTO(token, roles));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data){
        if(this.userRepository.findByLogin(data.getLogin()).isPresent()) return ResponseEntity.badRequest().build();
    
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());
    
        Set<Role> roles = data.getRoles().stream()
                              .map(roleName -> {
                                  return roleRepository.findByName(roleName)
                                          .orElseThrow(() -> new IllegalArgumentException("Role " + roleName + " not found"));
                              })
                              .collect(Collectors.toSet());
    
        User newUser = new User(data.getLogin(), encryptedPassword, roles);
    
        this.userRepository.save(newUser);
    
        return ResponseEntity.ok().build();
    }
    
}
