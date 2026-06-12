package com.utp.RestoControl.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security para RestoControl
 * 
 * Esta configuración implementa autenticación y autorización
 * basada en roles (ROLE_ADMIN, ROLE_MESERO, ROLE_COCINA, etc.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Codificador de contraseñas usando BCrypt
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración de la cadena de filtros de seguridad
     * Define qué rutas son públicas y cuáles requieren autenticación
     * 
     * @param http HttpSecurity configuración
     * @return SecurityFilterChain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Autorización de solicitudes
                .authorizeHttpRequests(authorize -> authorize
                        // Rutas públicas - accesibles sin autenticación
                        .requestMatchers("/login", "/registro", "/api/auth/**").permitAll()
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()
                        
                        // Rutas protegidas - requieren autenticación
                        .requestMatchers("/dashboard", "/menu", "/pedidos", "/mesas").authenticated()
                        
                        // Rutas administrativas - requieren rol ADMIN
                        .requestMatchers("/usuarios-roles", "/configuracion", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**", "/api/roles/**").hasRole("ADMIN")
                        
                        // Rutas de gestión de pedidos - MESERO, ADMIN, COCINA
                        .requestMatchers("/api/pedidos/**").hasAnyRole("MESERO", "ADMIN", "COCINA")
                        
                        // Rutas de gestión de mesas - MESERO, ADMIN
                        .requestMatchers("/api/mesas/**").hasAnyRole("MESERO", "ADMIN")
                        
                        // Rutas de menú - ADMIN, MESERO
                        .requestMatchers("/api/alimentos/**", "/api/categorias/**").hasAnyRole("ADMIN", "MESERO")
                        
                        // Cualquier otra solicitud requiere autenticación
                        .anyRequest().authenticated()
                )
                
                // Configuración del formulario de login
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("correo")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                
                // Configuración del logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                
                // Configuración de sesiones
                .sessionManagement(session -> session
                        .sessionFixationProtection()
                        .maximumSessions(1)
                        .expiredUrl("/login")
                )
                
                // CORS
                .cors(cors -> cors.configurationSource(request -> {
                    org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                    config.setAllowedOrigins(java.util.List.of("http://localhost:8080"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(java.util.List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                
                // CSRF - deshabilitado por ahora para testing con API REST
                .csrf(csrf -> csrf.disable())
                
                // Headers de seguridad
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .xssProtection()
                        .and()
                        .contentSecurityPolicy("default-src 'self'")
                );

        return http.build();
    }

    /**
     * Configuración del AuthenticationManager
     * Utiliza el UserDetailsService para cargar datos de usuario
     * 
     * @param http HttpSecurity
     * @param passwordEncoder PasswordEncoder
     * @param userDetailsService UserDetailsService
     * @return AuthenticationManager
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService
    ) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }
}
