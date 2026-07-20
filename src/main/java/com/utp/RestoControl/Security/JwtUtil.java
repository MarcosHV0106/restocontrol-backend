package com.utp.RestoControl.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);
    private final SecretKey signingKey;
    private final long expirationTime;

    public JwtUtil(
            @Value("${restocontrol.jwt.secret:}") String secretKey,
            @Value("${restocontrol.jwt.expiration-ms:86400000}") long expirationTime
    ) {
        if (secretKey == null || secretKey.isBlank()) {
            this.signingKey = Jwts.SIG.HS256.key().build();
            LOGGER.warn("JWT_SECRET no configurado; se usara una clave efimera hasta reiniciar la aplicacion.");
        } else {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey.trim()));
        }
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("La expiracion del JWT debe ser mayor que cero.");
        }
        this.expirationTime = expirationTime;
    }

    // Convierte la firma en un objeto SecretKey usando la especificación HMAC-SHA
    private SecretKey getSigningKey() {
        return signingKey;
    }

    // Extrae el correo/username del token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Método genérico para extraer cualquier "Claim" (propiedad) del token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Genera el token e inyecta los roles del usuario para que el Frontend (Vue) los use directamente
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList());

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    // Valida si el token pertenece al usuario y no ha expirado
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // Parsea el token utilizando la API moderna de JJWT 0.13.0
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload(); // .getPayload() reemplaza al antiguo .getBody()
    }
}
