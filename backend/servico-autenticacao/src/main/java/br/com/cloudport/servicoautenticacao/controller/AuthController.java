package br.com.cloudport.servicoautenticacao.controller;

import br.com.cloudport.servicoautenticacao.model.AuthenticationRequest;
import br.com.cloudport.servicoautenticacao.model.AuthenticationResponse;
import br.com.cloudport.servicoautenticacao.service.CustomUserDetailsService;
import br.com.cloudport.servicoautenticacao.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {

        logger.warn("AuthController: método createAuthenticationToken chamado");
        logger.warn("AuthController: Received authentication request: {}", authenticationRequest);
        logger.warn("AuthController: Received authentication request.getUsername(): {}", authenticationRequest.getUsername());
        logger.warn("AuthController: Received authentication request.getPassword(): {}", authenticationRequest.getPassword());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
            );
            // Registro de log de sucesso
            logger.warn("AuthController: Authentication successful for username: {}", authenticationRequest.getUsername());

        } catch (BadCredentialsException e) {
            logger.warn("AuthController: Usuário ou senha incorretos", e);
            throw new Exception("Usuário ou senha incorretos", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        logger.warn("AuthController: UserDetails - {}", userDetails);

        final String jwt = jwtUtil.generateToken(userDetails);
        logger.warn("AuthController: JWT gerado - {}", jwt);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }
}
