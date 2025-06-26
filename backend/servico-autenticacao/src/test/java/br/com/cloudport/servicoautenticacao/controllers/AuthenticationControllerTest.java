package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.config.TokenService;
import br.com.cloudport.servicoautenticacao.dto.AuthenticationDTO;
import br.com.cloudport.servicoautenticacao.dto.RegisterDTO;
import br.com.cloudport.servicoautenticacao.model.Role;
import br.com.cloudport.servicoautenticacao.model.User;
import br.com.cloudport.servicoautenticacao.model.UserRole;
import br.com.cloudport.servicoautenticacao.repositories.RoleRepository;
import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
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
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private TokenService tokenService;

    @Test
    void login_success() throws Exception {
        Role role = new Role("ADMIN");
        UserRole userRole = new UserRole(role);
        Set<UserRole> roles = Collections.singleton(userRole);
        User user = new User("test", "pass", roles);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.generateToken(user)).thenReturn("token");

        AuthenticationDTO dto = new AuthenticationDTO("test", "pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void login_invalidCredentials() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        AuthenticationDTO dto = new AuthenticationDTO("bad", "creds");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateLogin() throws Exception {
        when(userRepository.findByLogin("john"))
                .thenReturn(Optional.of(new User()));

        RegisterDTO dto = new RegisterDTO("john", "pass", Collections.singleton("ADMIN"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_success() throws Exception {
        when(userRepository.findByLogin("john")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));

        RegisterDTO dto = new RegisterDTO("john", "pass", Collections.singleton("ADMIN"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userRepository).save(any(User.class));
    }
}
