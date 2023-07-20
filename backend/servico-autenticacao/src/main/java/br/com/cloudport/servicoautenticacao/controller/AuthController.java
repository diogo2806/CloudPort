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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @CrossOrigin(origins = "https://4200-diogo2806-cloudport-5rk6q3wf87j.ws-us102.gitpod.io", methods = {RequestMethod.OPTIONS, RequestMethod.POST})
    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {

        System.out.println("AuthController: método createAuthenticationToken chamado");


        logger.warn("Received authentication request: {}", authenticationRequest);
        logger.warn("Received authentication request.getUsername(): {}", authenticationRequest.getUsername());
        logger.warn("Received authentication request.getPassword(): {}", authenticationRequest.getPassword());

        try {
            authenticationManager.authenticate(
                    //new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
                    new UsernamePasswordAuthenticationToken("gitpod", "gitpod")
            );
        }
        catch (BadCredentialsException e) {
            System.out.println("AuthController: BadCredentialsException - " + e.getMessage());

            logger.warn("Usuário ou senha incorretos", e);
            throw new Exception("Usuário ou senha incorretos", e);
        }

        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authenticationRequest.getUsername());

        final String jwt = jwtUtil.generateToken(userDetails);
        System.out.println("AuthController: JWT gerado - " + jwt);
        logger.warn("AuthController: JWT gerado - " + jwt);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
       
    }
}
