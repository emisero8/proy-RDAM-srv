package ar.gob.rdam.auth.service;

import ar.gob.rdam.domain.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // segundos

    @Value("${rdam.auth.ciudadano-token-expiration:86400}")
    private long ciudadanoTokenExpiration; // segundos (default 24hs)

    // ─── Generación — Operador Interno ──────────────────────────────────────────

    /**
     * JWT para operadores internos: incluye rol, tipo, portal y circunscripción.
     */
    public String generateAccessToken(Usuario usuario, String portal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().name());
        claims.put("tipo", usuario.getTipo().name());
        claims.put("portal", portal);
        if (usuario.getCircunscripcion() != null) {
            claims.put("circunscripcion", usuario.getCircunscripcion());
        }
        return buildToken(claims, usuario.getEmail(), accessTokenExpiration);
    }

    // ─── Generación — Ciudadano ─────────────────────────────────────────────────

    /**
     * JWT simplificado para ciudadanos: solo email y tipo, con expiración de 24hs.
     */
    public String generateCiudadanoToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", "CIUDADANO");
        claims.put("portal", "CIUDADANO");
        claims.put("rol", "CIUDADANO");
        return buildToken(claims, usuario.getEmail(), ciudadanoTokenExpiration);
    }

    // ─── Builder interno ────────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationSeconds) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000L))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Validación ────────────────────────────────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get("rol", String.class));
    }

    public String extractPortal(String token) {
        return extractClaim(token, claims -> claims.get("portal", String.class));
    }

    public String extractTipo(String token) {
        return extractClaim(token, claims -> claims.get("tipo", String.class));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getCiudadanoTokenExpiration() {
        return ciudadanoTokenExpiration;
    }
}
