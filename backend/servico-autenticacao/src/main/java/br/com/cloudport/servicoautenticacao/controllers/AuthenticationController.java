package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.domain.user.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.domain.user.LoginResponseDTO;
import br.com.cloudport.servicoautenticacao.domain.user.RegisterDTO;
import br.com.cloudport.servicoautenticacao.domain.user.User;
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

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository repository;
    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.getLogin(), data.getPassword());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token, data.getRole()));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByLogin(data.getLogin()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.getPassword());
        User newUser = new User(data.getLogin(), encryptedPassword, data.getRole());

        this.repository.save(newUser);

        return ResponseEntity.ok().build();
    }
}
