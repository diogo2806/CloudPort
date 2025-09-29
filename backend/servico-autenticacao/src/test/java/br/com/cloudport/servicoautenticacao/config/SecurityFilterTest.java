package br.com.cloudport.servicoautenticacao.config;

import br.com.cloudport.servicoautenticacao.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityFilterTest {

    private TokenService tokenService;
    private UserRepository userRepository;
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        userRepository = mock(UserRepository.class);
        securityFilter = new SecurityFilter(tokenService, userRepository);
    }

    @Test
    void invalidTokenShouldNotInvokeRepositoryAndReturnUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/products");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(tokenService.validateToken("invalid-token")).thenReturn(Optional.empty());

        securityFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(userRepository, never()).findByLogin(any());
        assertNull(filterChain.getRequest());
    }
}
