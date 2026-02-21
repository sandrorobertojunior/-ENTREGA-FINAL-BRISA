package org.obeci.platform.configs;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor do handshake do WebSocket.
 *
 * <p>Responsabilidade: extrair o JWT do cookie HttpOnly (ou do header Authorization)
 * durante o handshake HTTP do WebSocket e armazenar uma {@link Authentication}
 * nos atributos da sessão WebSocket.</p>
 *
 * <p>Por que isso existe se já temos filtro HTTP?
 * <ul>
 *   <li>O filtro HTTP autentica a requisição, mas o contexto de segurança não é automaticamente
 *       transferido para a sessão STOMP.</li>
 *   <li>Ao salvar a Authentication em attributes, conseguimos associá-la no CONNECT STOMP.</li>
 * </ul>
 * </p>
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Chave usada em sessionAttributes para transportar a Authentication do handshake para o STOMP CONNECT. */
    public static final String ATTR_AUTH = "WS_AUTH";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenCookieService tokenCookieService;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService, TokenCookieService tokenCookieService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenCookieService = tokenCookieService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletReq)) {
            return true;
        }

        HttpServletRequest http = servletReq.getServletRequest();

        // 1) Preferência: cookie HttpOnly (token)
        String token = null;
        if (http.getCookies() != null) {
            String cookieName = tokenCookieService.getCookieName();
            for (Cookie c : http.getCookies()) {
                if (cookieName.equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // 2) Fallback: Authorization: Bearer ... (útil se o cliente não usa cookie)
        if (token == null) {
            String auth = http.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }

        if (token != null && !token.isBlank()) {
            try {
                String username = jwtUtil.extractUsername(token);
                UserDetails details = userDetailsService.loadUserByUsername(username);

                // Authorities vêm do UserDetails (compatível com suas roles atuais).
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        details,
                        null,
                        details.getAuthorities()
                );

                attributes.put(ATTR_AUTH, authentication);
            } catch (Exception ignored) {
                // Não bloqueamos o handshake aqui; a API /ws já é protegida por SecurityFilterChain.
                // Se o token estiver inválido, a conexão pode existir mas não conseguirá publicar updates.
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // Nada a fazer.
    }
}
