package org.obeci.platform.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Propriedades de CORS da aplicação.
 *
 * <p>Este bean faz o bind das configurações sob o prefixo {@code app.cors} (application*.yml)
 * para listas de origens, métodos e headers permitidos.</p>
 *
 * <p>Dependências/relações:
 * <ul>
 *   <li>Consumido por {@link SecurityConfiguration#corsConfigurationSource()}.</li>
 * </ul>
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    private List<String> allowedOrigins = Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001"
    );

    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = Arrays.asList("*");

    private boolean allowCredentials = true;

    // Observação: se allow-credentials=true, não use "*" em allowed-origins.
    // Em produção, mantenha allowed-origins explícito.

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
