package org.obeci.platform.configs;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
/**
 * Filtro de autenticação JWT.
 *
 * <p>Responsável por extrair o token JWT da request (preferencialmente via cookie HttpOnly,
 * com fallback para {@code Authorization: Bearer ...}), validar assinatura/expiração e
 * popular o {@link org.springframework.security.core.context.SecurityContext} com um
 * {@link UsernamePasswordAuthenticationToken} quando o token é válido.</p>
 *
 * <p>Efeitos colaterais importantes:
 * <ul>
 *   <li>Em caso de token inválido/expirado, adiciona header {@code Set-Cookie} para limpar o cookie.</li>
 *   <li>Define a autenticação no SecurityContext para a thread da request.</li>
 * </ul>
 * </p>
 */
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenCookieService tokenCookieService;

    @Override
    /**
     * Executa a autenticação por request (OncePerRequestFilter).
     *
     * <p>Fluxo:
     * <ol>
     *   <li>Extrai JWT do header Bearer ou cookie.</li>
     *   <li>Extrai username (subject) via {@link JwtUtil}.</li>
     *   <li>Carrega {@link UserDetails} e valida o token.</li>
     *   <li>Se inválido/expirado, sinaliza para limpar cookie.</li>
     * </ol>
     * </p>
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        boolean clearCookie = false;

        // Compatibilidade: aceita Bearer token no header Authorization.
        // Preferência (principalmente em produção): cookie HttpOnly (configurável em app.auth.cookie.*).
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
        } else {
            // Produção: preferir cookie HttpOnly com nome "token"
            // O cookie não é acessível via JS, mas o servidor consegue ler aqui.
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                    if (tokenCookieService.getCookieName().equals(c.getName())) {
                        jwtToken = c.getValue();
                        break;
                    }
                }
            }
        }

        // Se houver token, tenta extrair o subject/username.
        if (jwtToken != null) {
            try {
                username = jwtUtil.extractUsername(jwtToken);
            } catch (IllegalArgumentException e) {
                System.out.println("Unable to get JWT Token");
                clearCookie = true;
            } catch (ExpiredJwtException e) {
                System.out.println("JWT Token has expired");
                clearCookie = true;
            } catch (SignatureException | MalformedJwtException e) {
                System.out.println("Invalid JWT signature or token format");
                clearCookie = true;
            }
        }

        // Somente autentica se ainda não houver autenticação na thread.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtToken != null && jwtUtil.validateToken(jwtToken, userDetails.getUsername())) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else if (jwtToken != null) {
                clearCookie = true;
            }
        }

        if (clearCookie) {
            // Token inválido/expirado: limpa o cookie para evitar loop de requests com credencial ruim.
            response.addHeader(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString());
        }
        chain.doFilter(request, response);
    }

}