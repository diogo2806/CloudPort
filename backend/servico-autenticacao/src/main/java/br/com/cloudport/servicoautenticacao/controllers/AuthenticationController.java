package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.LoginResponseDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.RegisterDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.validacao.SanitizadorEntrada;
import br.com.cloudport.servicoautenticacao.app.administracao.dto.UsuarioInfoDTO;
import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import br.com.cloudport.servicoautenticacao.repositories.UsuarioPapelRepositorio;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepositorio usuarioRepositorio;
    private final PapelRepositorio papelRepositorio;
    private final TokenService tokenService;
    private final UsuarioPapelRepositorio usuarioPapelRepositorio;

    @Value("${api.security.self-registration.allowed-roles:USER}")
    private String papeisPermitidosAutoCadastro;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                     UsuarioRepositorio usuarioRepositorio,
                                     PapelRepositorio papelRepositorio,
                                     TokenService tokenService,
                                     UsuarioPapelRepositorio usuarioPapelRepositorio) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepositorio = usuarioRepositorio;
        this.papelRepositorio = papelRepositorio;
        this.tokenService = tokenService;
        this.usuarioPapelRepositorio = usuarioPapelRepositorio;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO dados){
        String loginSanitizado;
        String senhaSanitizada;
        try {
            loginSanitizado = SanitizadorEntrada.sanitizarLogin(dados.getLogin());
            senhaSanitizada = SanitizadorEntrada.sanitizarSenha(dados.getSenha());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        var usernamePassword = new UsernamePasswordAuthenticationToken(loginSanitizado, senhaSanitizada);
        var autenticacao = this.authenticationManager.authenticate(usernamePassword);

        Usuario usuario = (Usuario) autenticacao.getPrincipal();
        var token = tokenService.generateToken(usuario);

        Set<String> papeis = java.util.Optional.ofNullable(usuario.getPapeis()).orElse(java.util.Collections.emptySet()).stream()
                                 .map(UsuarioPapel::getPapel)
                                 .map(Papel::getNome)
                                 .filter(StringUtils::hasText)
                                 .map(nomePapel -> nomePapel.startsWith("ROLE_") ? nomePapel : "ROLE_" + nomePapel.toUpperCase())
                                 .collect(Collectors.toCollection(LinkedHashSet::new));

        String perfil = papeis.stream().findFirst().orElse("");
        String nome = StringUtils.hasText(usuario.getNome()) ? usuario.getNome() : usuario.getLogin();

        return ResponseEntity.ok(new LoginResponseDTO(
                usuario.getId(),
                usuario.getLogin(),
                nome,
                perfil,
                token,
                papeis,
                usuario.getTransportadoraDocumento(),
                usuario.getTransportadoraNome()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registrar(@RequestBody @Valid RegisterDTO dados){
        if (this.usuarioRepositorio.findByLogin(dados.getLogin()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login informado já está em uso.");
        }

        String senhaCriptografada = new BCryptPasswordEncoder().encode(dados.getPassword());

        Set<String> papeisPermitidosNormalizados = Arrays.stream(papeisPermitidosAutoCadastro.split(","))
                .map(String::trim)
                .filter(papel -> !papel.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), conjunto -> conjunto.isEmpty() ? Collections.singleton("USER") : conjunto));

        Set<String> papeisSolicitados = dados.getRoles().stream()
                .map(String::trim)
                .filter(papel -> !papel.isEmpty())
                .collect(Collectors.toSet());

        if (!papeisPermitidosNormalizados.containsAll(papeisSolicitados)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Papéis informados não são permitidos para auto-registro.");
        }

        Set<UsuarioPapel> papeis = papeisSolicitados.stream()
                              .map(nomePapel -> {
                                  Papel papel = papelRepositorio.findByNome(nomePapel)
                                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel '" + nomePapel + "' não encontrado."));
                                  return new UsuarioPapel(null, papel);
                              })
                              .collect(Collectors.toSet());

        if (papeis.isEmpty()) {
            papeis = papeisPermitidosNormalizados.stream()
                    .map(nomePapel -> papelRepositorio.findByNome(nomePapel)
                            .map(papel -> new UsuarioPapel(null, papel))
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Papel '" + nomePapel + "' não encontrado.")))
                    .collect(Collectors.toSet());
        }

        Usuario novoUsuario = new Usuario(dados.getLogin(), senhaCriptografada, dados.getNome(),
                dados.getTransportadoraDocumento(), dados.getTransportadoraNome(), papeis);

        papeis.forEach(papel -> papel.setUsuario(novoUsuario));

        this.usuarioRepositorio.save(novoUsuario);
        this.usuarioPapelRepositorio.saveAll(papeis);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/usuarios/{login}")
    @PreAuthorize("hasRole('ADMIN_PORTO')")
    public ResponseEntity<UsuarioInfoDTO> buscarUsuario(@PathVariable String login) {
        Usuario usuario = usuarioRepositorio.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return ResponseEntity.ok(UsuarioInfoDTO.fromUsuario(usuario));
    }
}
