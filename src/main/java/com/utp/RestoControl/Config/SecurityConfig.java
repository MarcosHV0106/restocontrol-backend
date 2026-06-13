package com.utp.RestoControl.Config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http

                // CSRF
                // Si usas formularios Thymeleaf, considera dejarlo habilitado.
                .csrf(csrf -> csrf.disable())

                // Autorización
                .authorizeHttpRequests(authorize -> authorize

                        // Públicas
                        .requestMatchers("/login", "/registro").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/logout", "/api/auth/verify").permitAll()
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/error", "/error/**").permitAll()

                        // Protegidas
                        .requestMatchers("/dashboard", "/menu", "/pedidos", "/mesas", "/configuracion")
                        .authenticated()

                        // Administración
                        .requestMatchers("/usuarios-roles").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**", "/api/roles/**").hasRole("ADMIN")

                        // Pedidos
                        .requestMatchers("/api/pedidos/**")
                        .hasAnyRole("MESERO", "ADMIN", "COCINA")

                        // Mesas
                        .requestMatchers("/api/mesas/**")
                        .hasAnyRole("MESERO", "ADMIN")

                        // Menú
                        .requestMatchers("/api/alimentos/**", "/api/categorias/**")
                        .hasAnyRole("ADMIN", "MESERO")

                        .anyRequest().authenticated()
                )

                // Login
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("correo")
                        .passwordParameter("clave")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // Sesiones
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .maximumSessions(1)
                )

                // CORS
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();

                    config.setAllowedOrigins(
                            List.of(
                                    "http://localhost:8080",
                                    "http://localhost:3000"
                            )
                    );

                    config.setAllowedMethods(
                            List.of(
                                    "GET",
                                    "POST",
                                    "PUT",
                                    "DELETE",
                                    "OPTIONS"
                            )
                    );

                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);

                    return config;
                }));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService
    ) throws Exception {

        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        builder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);

        return builder.build();
    }
}