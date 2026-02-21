package org.obeci.platform.configs;
import org.obeci.platform.services.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtro de autenticação alternativo baseado em {@code Authorization: Basic ...}.
 *
 * <p>Este filtro tenta validar credenciais Basic (email:senha) usando o fluxo de login do
 * {@link UsuarioService}. Caso válido, popula o {@link org.springframework.security.core.context.SecurityContext}
 * com uma autenticação simples (ROLE_USER).</p>
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>Este filtro <strong>não</strong> possui anotações (@Component/@Bean) neste projeto. Portanto,
 *   só terá efeito se for explicitamente registrado na cadeia de filtros do Spring Security.</li>
 *   <li>Usa {@code System.out.println} para logging (não estruturado).</li>
 * </ul>
 * </p>
 *
 * <p>Efeitos colaterais: pode autenticar a request ao setar o SecurityContext.</p>
 */
public class CustomAuthFilter extends OncePerRequestFilter {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthFilter(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Se existir header Basic, tenta validar as credenciais e autenticar a request.
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                // Extrai o token (remove "Basic ")
                String base64Credentials = authHeader.substring(6);

                // Decodifica Base64
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(decodedBytes);

                // Separa email e senha
                String[] parts = credentials.split(":", 2);
                if (parts.length == 2) {
                    String email = parts[0];
                    String password = parts[1];

                    // Usa o mesmo método do login para validar (evita duplicar regras de senha/hash).
                    boolean isValid = usuarioService.login(email, password).isPresent();

                    if (isValid) {
                        // Cria a autenticação no Spring Security.
                        // Observação: aqui não é carregado UserDetails completo; apenas o "principal" email.
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Credenciais inválidas - não define autenticação.
                        System.out.println("Credenciais inválidas para: " + email);
                    }
                }
            } catch (Exception e) {
                System.out.println("Erro ao processar token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
