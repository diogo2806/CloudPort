package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.app.configuracoes.dto.RegisterDTO;
import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.model.Papel;
import br.com.cloudport.servicoautenticacao.model.Usuario;
import br.com.cloudport.servicoautenticacao.model.UsuarioPapel;
import br.com.cloudport.servicoautenticacao.repositories.PapelRepositorio;
import br.com.cloudport.servicoautenticacao.repositories.UsuarioPapelRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UsuarioRepositorio usuarioRepositorio;

    @MockBean
    private PapelRepositorio papelRepositorio;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UsuarioPapelRepositorio usuarioPapelRepositorio;

    @Test
    void login_sucesso() throws Exception {
        Papel papel = new Papel("ADMIN");
        UsuarioPapel usuarioPapel = new UsuarioPapel(papel);
        Set<UsuarioPapel> papeis = Collections.singleton(usuarioPapel);
        Usuario usuario = new Usuario("test", "pass", papeis);

        Authentication autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(autenticacao);
        when(tokenService.generateToken(usuario)).thenReturn("token");

        AuthenticationDTO dto = new AuthenticationDTO("test", "pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId().toString()))
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.nome").value("test"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void login_credenciaisInvalidas() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        AuthenticationDTO dto = new AuthenticationDTO("bad", "creds");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_payloadInvalido_retornaErrosDeValidacao() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validação dos dados enviados."))
                .andExpect(jsonPath("$.erros.login").value("O login é obrigatório."))
                .andExpect(jsonPath("$.erros.password").value("A senha é obrigatória."));
    }

    @Test
    void login_camposCurtos_retornaErrosDeValidacao() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("ab", "123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validação dos dados enviados."))
                .andExpect(jsonPath("$.erros.login").value("O login deve ter entre 3 e 100 caracteres."))
                .andExpect(jsonPath("$.erros.password").value("A senha deve ter pelo menos 6 caracteres."));
    }

    @Test
    void registrar_loginDuplicado() throws Exception {
        when(usuarioRepositorio.findByLogin("john"))
                .thenReturn(Optional.of(new Usuario()));

        RegisterDTO dto = new RegisterDTO("john", "pass", Collections.singleton("ADMIN"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Login informado já está em uso."));
    }

    @Test
    void registrar_payloadInvalido_retornaErros() throws Exception {
        RegisterDTO dto = new RegisterDTO("", "", Collections.emptySet());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validação dos dados enviados."))
                .andExpect(jsonPath("$.erros.login").value("O login é obrigatório."))
                .andExpect(jsonPath("$.erros.password").value("A senha é obrigatória."))
                .andExpect(jsonPath("$.erros.roles").value("Informe ao menos uma role."));
    }

    @Test
    void registrar_roleEmBranco_retornaErroDeElemento() throws Exception {
        Set<String> roles = new LinkedHashSet<>();
        roles.add(" ");
        roles.add("ADMIN");

        RegisterDTO dto = new RegisterDTO("usuario", "segredo", roles);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Erro de validação dos dados enviados."))
                .andExpect(jsonPath("$.erros['roles[0]']").value("A role não pode ser vazia."));
    }

    @Test
    void registrar_sucesso() throws Exception {
        when(usuarioRepositorio.findByLogin("john")).thenReturn(Optional.empty());
        when(papelRepositorio.findByNome("ADMIN")).thenReturn(Optional.of(new Papel("ADMIN")));

        RegisterDTO dto = new RegisterDTO("john", "pass", Collections.singleton("ADMIN"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(usuarioRepositorio).save(any(Usuario.class));

        ArgumentCaptor<Iterable<UsuarioPapel>> papeisCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(usuarioPapelRepositorio).saveAll(papeisCaptor.capture());

        Iterable<UsuarioPapel> papeisSalvos = papeisCaptor.getValue();
        assertNotNull(papeisSalvos);
        assertTrue(papeisSalvos.iterator().hasNext());
        UsuarioPapel papelSalvo = papeisSalvos.iterator().next();
        assertNotNull(papelSalvo.getUsuario());
        assertEquals("john", papelSalvo.getUsuario().getLogin());
    }

    @Test
    void registrar_papelNaoEncontrado() throws Exception {
        when(usuarioRepositorio.findByLogin("john")).thenReturn(Optional.empty());
        when(papelRepositorio.findByNome("UNKNOWN")).thenReturn(Optional.empty());

        RegisterDTO dto = new RegisterDTO("john", "pass", Collections.singleton("UNKNOWN"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Papel 'UNKNOWN' não encontrado."));
    }
}
