package br.com.abadeus.paellaapi.config;

import br.com.abadeus.paellaapi.security.JwtAuthFilter;
import br.com.abadeus.paellaapi.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/registrar", "/api/status", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/eventos", "/api/eventos/**").permitAll()
                        // Equipe de entrega (voluntario) so acessa a validacao de retirada
                        .requestMatchers("/api/admin/retiradas/**", "/api/admin/eventos/*/retiradas", "/api/sync/**")
                        .hasAnyRole("ADMINISTRADOR", "VOLUNTARIO")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escreverErro(res, 401, "Nao autenticado", "Faca login para acessar este recurso."))
                        .accessDeniedHandler((req, res, e) ->
                                escreverErro(res, 403, "Acesso negado", "Seu perfil nao tem permissao para esta acao.")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void escreverErro(jakarta.servlet.http.HttpServletResponse res, int status,
                                     String erro, String mensagem) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"status\":" + status + ",\"erro\":\"" + erro
                + "\",\"mensagem\":\"" + mensagem + "\"}");
    }
}
