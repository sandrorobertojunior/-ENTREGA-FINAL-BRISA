package org.obeci.platform.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
/**
 * Utilitário responsável por criar e validar tokens JWT (HS256).
 *
 * <p>Principais responsabilidades:
 * <ul>
 *   <li>Construir a chave de assinatura a partir de {@link JwtProperties#getSecret()}.</li>
 *   <li>Gerar tokens com subject = username/email e expiração configurável.</li>
 *   <li>Extrair claims (subject/expiração) e validar token.</li>
 * </ul>
 * </p>
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>Se {@code app.jwt.secret} estiver vazio e {@code requireSecret=false}, uma chave aleatória é usada;
 *   isso invalida tokens após restart.</li>
 *   <li>Para HS256, o secret precisa ter no mínimo 32 bytes.</li>
 * </ul>
 * </p>
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final JwtProperties jwtProperties;
    private final Key secretKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = buildKey(jwtProperties.getSecret());
    }

    private Key buildKey(String secret) {
        // Constrói a chave de assinatura. Em produção, deve ser estável e configurada externamente.
        if (secret == null || secret.isBlank()) {
            if (jwtProperties.isRequireSecret()) {
                throw new IllegalStateException("app.jwt.secret é obrigatório (require-secret=true)");
            }
            log.warn("app.jwt.secret não definido; usando chave aleatória (tokens vão invalidar ao reiniciar a API). Defina app.jwt.secret em produção.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret muito curto para HS256 (mínimo 32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        // Subject é usado como "username" (no projeto, tipicamente email).
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        // Atualmente gera token com apenas subject + timestamps; claims custom podem ser adicionadas no futuro.
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // Calcula expiração a partir de app.jwt.expiration-seconds.
        long expiresInMs = jwtProperties.getExpirationSeconds() * 1000L;
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiresInMs))
            .signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    public Boolean validateToken(String token, String username) {
        // Validação mínima: subject corresponde ao username e token não expirou.
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}