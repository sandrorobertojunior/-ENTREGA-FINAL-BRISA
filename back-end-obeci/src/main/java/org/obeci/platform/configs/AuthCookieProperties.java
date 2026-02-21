package org.obeci.platform.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades do cookie de autenticação.
 *
 * <p>Mapeia configurações sob {@code app.auth.cookie} para controlar nome, flags de segurança
 * (HttpOnly/Secure/SameSite), escopo (path/domain) e expiração (Max-Age) do cookie que
 * armazena o JWT.</p>
 *
 * <p>Dependências/relações:
 * <ul>
 *   <li>Consumido por {@link TokenCookieService} para criar/limpar cookies.</li>
 *   <li>Lido indiretamente pelo filtro {@link JwtRequestFilter} ao buscar o cookie pelo nome.</li>
 * </ul>
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "app.auth.cookie")
public class AuthCookieProperties {

    private String name = "token";
    private boolean httpOnly = true;
    private boolean secure = false;
    private String sameSite = "Lax";
    private String path = "/";
    /**
     * Max-Age do cookie em segundos.
     * Por padrão, acompanha a expiração do JWT (10h), mas é recomendado reduzir em produção.
     */
    private Long maxAgeSeconds = 60L * 60L * 10L;
    private String domain;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(Long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
