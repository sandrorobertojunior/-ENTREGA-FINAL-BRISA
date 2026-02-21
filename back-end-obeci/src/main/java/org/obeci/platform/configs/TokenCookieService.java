package org.obeci.platform.configs;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
/**
 * Serviço utilitário para criar e limpar o cookie que carrega o JWT.
 *
 * <p>Centraliza a construção de {@link ResponseCookie} para garantir consistência entre
 * ambientes e evitar duplicação de regras de cookie em controllers/filtros.</p>
 *
 * <p>Dependências/relações:
 * <ul>
 *   <li>Lê {@link AuthCookieProperties}.</li>
 *   <li>Usado por {@link JwtRequestFilter} para limpar cookie quando token é inválido.</li>
 *   <li>Tipicamente usado por controllers (ex.: login/logout) para setar/limpar o cookie.</li>
 * </ul>
 * </p>
 */
public class TokenCookieService {

    private final AuthCookieProperties props;

    public TokenCookieService(AuthCookieProperties props) {
        this.props = props;
    }

    public String getCookieName() {
        return props.getName();
    }

    public ResponseCookie createAuthCookie(String token) {
        // Centraliza a criação do cookie do JWT para evitar duplicação e garantir
        // que DEV/PROD usem as flags corretas (secure/sameSite/domain/maxAge).
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(props.getName(), token)
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .sameSite(props.getSameSite())
                .path(props.getPath())
                .maxAge(props.getMaxAgeSeconds());

        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            builder.domain(props.getDomain());
        }

        return builder.build();
    }

    public ResponseCookie clearAuthCookie() {
        // Cookie expirado (Max-Age=0) para logout/invalidar sessão do cliente.
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(props.getName(), "")
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .sameSite(props.getSameSite())
                .path(props.getPath())
                .maxAge(0);

        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            builder.domain(props.getDomain());
        }

        return builder.build();
    }
}
