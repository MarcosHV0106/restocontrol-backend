package com.utp.RestoControl.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtUtilTests {

    private static final String CLAVE_PRUEBA = Base64.getEncoder().encodeToString(
            "restocontrol-clave-pruebas-32bytes-segura".getBytes(StandardCharsets.UTF_8));

    @Test
    void generaYValidaUnTokenConLaClaveConfigurada() {
        JwtUtil jwtUtil = new JwtUtil(CLAVE_PRUEBA, 60_000);
        UserDetails usuario = User.withUsername("admin@restocontrol.test")
                .password("no-se-expone")
                .roles("ADMIN")
                .build();

        String token = jwtUtil.generateToken(usuario);

        assertEquals("admin@restocontrol.test", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token, usuario));
    }

    @Test
    void rechazaUnaExpiracionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(CLAVE_PRUEBA, 0));
    }
}
