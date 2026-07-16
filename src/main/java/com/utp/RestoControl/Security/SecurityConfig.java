package com.utp.RestoControl.Security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Tendrás que inyectar el filtro JWT que crearemos (ejemplo de inyección)
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${restocontrol.logging.request-id-header:X-Request-ID}")
    private String requestIdHeader;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF se deshabilita porque JWT no es vulnerable a CSRF de la misma forma que las cookies
                .csrf(csrf -> csrf.disable())

                // Autorización de rutas modificada a Autoridades directas
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/registro", "/api/auth/activaciones/**").permitAll()
                        // Nota: Al usar hasAnyRole, NO pongas el prefijo "ROLE_" aquí,
                        // Spring lo añade automáticamente por ti.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reportes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alimentos/*/receta").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/alimentos/*/receta").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/insumos", "/api/insumos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/insumos", "/api/insumos/**").hasRole("ADMIN")
                        .requestMatchers("/api/pedidos", "/api/pedidos/**").hasAnyRole("MESERO", "ADMIN", "COCINA")
                        .requestMatchers("/api/mesas/**").hasAnyRole("MESERO", "ADMIN")
                        .requestMatchers("/api/alimentos/**", "/api/categorias/**").hasAnyRole("ADMIN", "MESERO")
                        .anyRequest().authenticated()
                )

                // IMPORTANTE: Cambiamos la política de creación de sesiones a STATELESS
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // CORS (Se mantiene igual para permitir que Vue se conecte)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:8080", "http://localhost:5173"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of(requestIdHeader));
                    config.setAllowCredentials(true);
                    return config;
                }))

                // IMPORTANTE: Añadimos el filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService
    ) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        return builder.build();
    }
}
