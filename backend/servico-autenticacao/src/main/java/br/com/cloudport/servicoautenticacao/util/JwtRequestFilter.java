package br.com.cloudport.servicoautenticacao.filter;

import br.com.cloudport.servicoautenticacao.service.CustomUserDetailsService;
import br.com.cloudport.servicoautenticacao.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

         // Log de exemplo
        logger.warn("JwtRequestFilter: doFilterInternal chamado");        
        logger.warn("JwtRequestFilter: HttpServletResponse response "+response);     
        logger.warn("JwtRequestFilter: HttpServletRequest request "+request);    
        logger.warn("JwtRequestFilter: FilterChain chain "+chain);   

        final String authorizationHeader = request.getHeader("Authorization");
        logger.warn("JwtRequestFilter: request.getHeader "+authorizationHeader);     


        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtUtil.getUsernameFromToken(jwt);
        }

        logger.warn("JwtRequestFilter: username "+username);   
        logger.warn("JwtRequestFilter: jwt "+jwt);   


        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (username.equals("gitpod")) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }
        }
        

        chain.doFilter(request, response);
    }
}
