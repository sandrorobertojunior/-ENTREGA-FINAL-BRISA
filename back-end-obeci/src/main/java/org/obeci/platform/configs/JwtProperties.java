package org.obeci.platform.configs;

/**
 * Propriedades de JWT da aplicação.
 *
 * <p>Mapeia configurações sob {@code app.jwt} (application*.yml) para controlar secret,
 * expiração e políticas de validação do secret.</p>
 *
 * <p>Dependências/relações:
 * <ul>
 *   <li>Consumido por {@link JwtUtil} para construir a chave e calcular expiração.</li>
 * </ul>
 * </p>
 */
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Secret usado para assinar o JWT (HS256). Em produção, defina via ENV/secret manager.
     * Deve ter pelo menos 32 bytes (>= 32 caracteres ASCII) para HS256.
     */
    private String secret;

    /**
     * Expiração do JWT em segundos.
     */
    private long expirationSeconds = 60L * 60L * 10L; // 10h

    /**
     * Se true, falha a inicialização caso app.jwt.secret esteja vazio.
     * Recomendado em produção.
     */
    private boolean requireSecret = false;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public boolean isRequireSecret() {
        return requireSecret;
    }

    public void setRequireSecret(boolean requireSecret) {
        this.requireSecret = requireSecret;
    }
}
