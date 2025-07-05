package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetails; // Importation ajoutée

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(String email, Long userId, String role) {
        String token = Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            boolean expired = isTokenExpired(token);
            return !expired;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Validation simple du jeton échouée : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valide le jeton JWT en vérifiant l'expiration et la correspondance avec les détails de l'utilisateur.
     * @param token Le jeton JWT à valider.
     * @param userDetails Les détails de l'utilisateur contre lesquels valider le jeton.
     * @return true si le jeton est valide pour l'utilisateur donné, false sinon.
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
       
        boolean isValid = (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
        if (!isValid) {
            logger.warn("Validation complète du jeton échouée pour l'email {}. Email match: {}, Expired: {}",
                email, email.equals(userDetails.getUsername()), isTokenExpired(token));
        }
        return isValid;
    }
}
