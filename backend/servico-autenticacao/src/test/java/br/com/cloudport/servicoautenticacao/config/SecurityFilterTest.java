package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.app.usuarioslista.UsuarioRepositorio;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    private SecurityFilter securityFilter;
    private TokenService tokenService;
    private UsuarioRepositorio usuarioRepositorio;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        usuarioRepositorio = mock(UsuarioRepositorio.class);
        securityFilter = new SecurityFilter(tokenService, usuarioRepositorio);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void deveRetornar401QuandoTokenInvalidoSemConsultarRepositorio() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenService.validateToken("invalid")).thenReturn(Optional.empty());

        securityFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(tokenService).validateToken("invalid");
        verifyNoInteractions(usuarioRepositorio);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void deveRetornar401QuandoTokenAusenteSemConsultarTokenService() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(usuarioRepositorio);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
