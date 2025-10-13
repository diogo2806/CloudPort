package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    private SecurityFilter securityFilter;
    private TokenService tokenService;
    private UserRepository userRepository;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        securityFilter = new SecurityFilter();
        tokenService = mock(TokenService.class);
        userRepository = mock(UserRepository.class);
        filterChain = mock(FilterChain.class);

        securityFilter.tokenService = tokenService;
        securityFilter.userRepository = userRepository;
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
        verifyNoInteractions(userRepository);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void deveRetornar401QuandoTokenAusenteSemConsultarTokenService() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(tokenService);
        verifyNoInteractions(userRepository);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
