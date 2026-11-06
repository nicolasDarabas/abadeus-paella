package br.com.abadeus.paellaapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le o cabecalho "Authorization: Bearer <token>" e autentica a requisicao.
 * Criado no SecurityConfig (nao e @Component para nao rodar duas vezes).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.validar(header.substring(7).trim()).ifPresent(usuario -> {
                var autoridade = new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name());
                var auth = new UsernamePasswordAuthenticationToken(usuario, null, List.of(autoridade));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
