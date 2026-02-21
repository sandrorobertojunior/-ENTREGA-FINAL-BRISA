package org.obeci.platform.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
/**
 * Configuração central de segurança (Spring Security).
 *
 * <p>Define:
 * <ul>
 *   <li>Política stateless (sem sessão de servidor).</li>
 *   <li>CORS a partir de {@link AppCorsProperties}.</li>
 *   <li>Autorização por rota (roles + autenticação).</li>
 *   <li>Filtro de autenticação JWT ({@link JwtRequestFilter}) antes do filtro padrão de username/password.</li>
 * </ul>
 * </p>
 */
public class SecurityConfiguration {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private AppCorsProperties corsProperties;

    // Bean para criptografia de senha
    @Bean
    /**
     * Encoder de senha para persistir/verificar senhas de usuários.
     *
     * <p>Uso: criação e validação de senha (BCrypt).</p>
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    /**
     * Cadeia de filtros de segurança.
     *
     * <p>Pontos críticos:
     * <ul>
     *   <li>{@code /auth/me} GET é aberto, mas retorna 401 se não autenticado (ver controller).</li>
     *   <li>{@code /api/usuarios/**} é restrito a ADMIN.</li>
     *   <li>Demais rotas (default) exigem autenticação.</li>
     * </ul>
     * </p>
     */
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Registro de usuário só pode ser realizado por ADMIN
                .requestMatchers(HttpMethod.POST, "/auth/register").hasRole("ADMIN")
                // Login e logout abertos (logout pode ser feito autenticado, mas manteremos aberto)
                .requestMatchers("/auth/login", "/auth/logout").permitAll()
                // /auth/me: GET pode ser aberto (retorna 401 se não houver token), PUT exige autenticação
                .requestMatchers(HttpMethod.GET, "/auth/me").permitAll()
                .requestMatchers(HttpMethod.PUT, "/auth/me").authenticated()
                // Lembretes do próprio usuário
                .requestMatchers("/auth/me/lembretes/**").authenticated()
                // CRUD de usuários somente por ADMIN
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                // Escola: leitura autenticada, escrita somente ADMIN
                .requestMatchers(HttpMethod.GET, "/api/escolas/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/escolas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/escolas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/escolas/**").hasRole("ADMIN")
                // Turma: leitura autenticada, escrita somente ADMIN
                .requestMatchers(HttpMethod.GET, "/api/turmas/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/turmas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/turmas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/turmas/**").hasRole("ADMIN")
                // Publicação: neste exemplo, exigimos autenticação para todos
                .requestMatchers("/api/publicacoes/**").authenticated()
                // Qualquer outro endpoint exige autenticação
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    /**
     * Exposição do {@link AuthenticationManager} padrão, para uso em fluxos de autenticação.
     */
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    /**
     * Fonte de configuração CORS aplicada para todas as rotas.
     *
     * <p>Usa {@link AppCorsProperties} para preencher allowed origins/methods/headers e allow-credentials.</p>
     */
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}