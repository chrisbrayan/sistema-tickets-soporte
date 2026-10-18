package com.utp.sistema_tickets.security;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Autenticación requerida"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, request, HttpServletResponse.SC_FORBIDDEN,
                                        "No tiene permisos para realizar esta operación")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/registro", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/usuarios/**", "/api/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tickets/search", "/api/tickets/filtrar",
                                "/api/tickets/resumen-por-estado", "/api/tickets/mis-asignados")
                        .hasAnyRole("TECNICO", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tickets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/**").hasAnyRole("TECNICO", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tickets/**").hasAnyRole("TECNICO", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/**").hasAnyRole("USUARIO", "TECNICO", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void writeSecurityError(HttpServletResponse response,
                                           HttpServletRequest request,
                                           int status,
                                           String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String body = "{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":" + status
                + ",\"error\":\"" + (status == 401 ? "Unauthorized" : "Forbidden")
                + "\",\"message\":\"" + message
                + "\",\"path\":\"" + request.getRequestURI() + "\"}";
        response.getWriter().write(body);
    }
}
