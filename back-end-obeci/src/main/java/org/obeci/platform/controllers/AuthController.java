package org.obeci.platform.controllers;

import org.obeci.platform.configs.JwtUtil;
import org.obeci.platform.configs.TokenCookieService;
import org.obeci.platform.entities.Usuario;
import org.obeci.platform.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import org.obeci.platform.dtos.UsuarioCreateRequest;
import org.obeci.platform.dtos.AuthLoginRequest;
import org.obeci.platform.dtos.UsuarioSelfUpdateRequest;
import org.obeci.platform.dtos.LembreteRequest;

import java.util.List;

@RestController
@RequestMapping("/auth")
/**
 * Controller de autenticação e autoatendimento do usuário logado.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Registrar usuário (restrito por SecurityConfiguration).</li>
 *   <li>Login: autentica credenciais e emite JWT em cookie HttpOnly.</li>
 *   <li>Logout: expira o cookie de autenticação.</li>
 *   <li>/me: ler/atualizar dados do próprio usuário autenticado.</li>
 *   <li>CRUD de lembretes do próprio usuário (índice no array).</li>
 * </ul>
 * </p>
 *
 * <p>Efeitos colaterais relevantes:
 * <ul>
 *   <li>Login/Logout/erros de token podem enviar {@code Set-Cookie}.</li>
 *   <li>Registro e atualização persistem dados no banco.</li>
 * </ul>
 * </p>
 */
public class AuthController {

    private final UsuarioService usuarioService;

    private final TokenCookieService tokenCookieService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthController(UsuarioService usuarioService, TokenCookieService tokenCookieService) {
        this.usuarioService = usuarioService;
        this.tokenCookieService = tokenCookieService;
    }

    // Registro de usuário
    @PostMapping("/register")
    /**
     * Registra um novo usuário.
     *
     * <p>Entrada: {@link UsuarioCreateRequest} (validado via Bean Validation).</p>
     * <p>Saída: entidade {@link Usuario} criada.</p>
     * <p>Erros: retorna 400 com mensagem (String) em caso de {@link RuntimeException}.</p>
     */
    public ResponseEntity<?> register(@Valid @RequestBody UsuarioCreateRequest request) {
        try {
            Usuario novoUsuario = usuarioService.register(request);
            return ResponseEntity.ok(novoUsuario);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    /**
     * Autentica email/senha e retorna JWT.
     *
     * <p>Entrada: {@link AuthLoginRequest}.</p>
     * <p>Saída: JSON com {@code token} e {@code username} + cookie HttpOnly com o JWT.</p>
     *
     * <p>Ponto crítico: em falha de autenticação, limpa cookie para evitar estado inconsistente.</p>
     */
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest usuario) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), usuario.getPassword())
            );
        } catch (Exception e) {
            // Falha de login: remove cookie de sessão para evitar estado inconsistênte no cliente.
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Usuário ou senha inválidos");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        final String jwt = jwtUtil.generateToken(userDetails.getUsername());

        Map<String, String> response = new HashMap<>();
        response.put("token", jwt);
        response.put("username", userDetails.getUsername());

        // Cookie HttpOnly é a forma preferida (principalmente em produção).
        // Manter "token" no corpo serve para compatibilidade com clientes antigos.
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, tokenCookieService.createAuthCookie(jwt).toString())
                .body(response);
    }

    // Endpoint de logout para limpar o cookie HttpOnly.
    @PostMapping("/logout")
    /**
     * Logout: expira o cookie de autenticação no cliente.
     */
    public ResponseEntity<?> logout() {
        // Logout: expira o cookie no cliente.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                .body("Logout realizado");
    }

    // Retorna informações do usuário autenticado, incluindo roles.
    @GetMapping("/me")
    /**
     * Retorna dados básicos do usuário autenticado.
     *
     * <p>Saída: JSON com {@code username}, {@code email}, {@code arrayRoles}.</p>
     * <p>Se não autenticado, retorna 401 e expira cookie (defensivo).</p>
     */
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            // Não autenticado: devolve 401 e garante limpeza de cookie.
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }
        String email = authentication.getName();
        return usuarioService.findByEmail(email)
                .map(u -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("username", u.getUsername());
                    dto.put("email", u.getEmail());
                    dto.put("arrayRoles", u.getArrayRoles());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    // Se o token foi aceito mas o usuário não existe mais, limpa cookie por segurança.
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Usuário não encontrado");
                    return ResponseEntity.status(404)
                            .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                            .body(err);
                });
    }

    // Atualiza dados do próprio usuário autenticado
    @PutMapping("/me")
    /**
     * Atualiza os dados do próprio usuário autenticado.
     *
     * <p>Entrada: {@link UsuarioSelfUpdateRequest} (validado).</p>
     * <p>Saída: JSON com dados atualizados.</p>
     *
     * <p>Ponto crítico: se o email for alterado, reemite JWT para refletir o novo subject.</p>
     */
    public ResponseEntity<?> updateMe(Authentication authentication, @Valid @RequestBody UsuarioSelfUpdateRequest request) {
        if (authentication == null || authentication.getName() == null) {
            // PUT /auth/me exige autenticação; por segurança, ainda limpamos cookie se não houver auth.
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }

        String email = authentication.getName();
        return usuarioService.findByEmail(email)
            .map(u -> {
                Usuario changes = new Usuario();
                changes.setUsername(request.getUsername());
                changes.setEmail(request.getEmail());
                changes.setPassword(request.getPassword());
                changes.setCpf(request.getCpf());

                boolean emailRequestedChange = request.getEmail() != null && !request.getEmail().equalsIgnoreCase(email);

                    return usuarioService.update(u.getId(), changes)
                            .map(updated -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("username", updated.getUsername());
                        dto.put("email", updated.getEmail());
                        dto.put("arrayRoles", updated.getArrayRoles());
                        if (emailRequestedChange) {
                            final String newJwt = jwtUtil.generateToken(updated.getEmail());
                            return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, tokenCookieService.createAuthCookie(newJwt).toString())
                                .body(dto);
                        }
                        return ResponseEntity.ok(dto);
                            })
                            .orElseGet(() -> {
                                Map<String, Object> err = new HashMap<>();
                                err.put("error", "Usuário não encontrado");
                                return ResponseEntity.status(404).body(err);
                            });
            })
                .orElseGet(() -> {
                    Map<String, Object> err = new HashMap<>();
                    err.put("error", "Usuário não encontrado");
                    return ResponseEntity.status(404).body(err);
                });
    }

    // =====================================================================
    // Lembretes do usuário autenticado
    // CRUD simples usando índice do array (0..n-1)
    // =====================================================================

    @GetMapping("/me/lembretes")
    /**
     * Lista os lembretes do usuário autenticado.
     *
     * <p>Saída: lista de strings (ordem preservada).</p>
     */
    public ResponseEntity<?> listLembretes(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }
        String email = authentication.getName();
        List<String> lembretes = usuarioService.listLembretes(email);
        return ResponseEntity.ok(lembretes);
    }

    @PostMapping("/me/lembretes")
    /**
     * Adiciona um lembrete ao final da lista do usuário autenticado.
     *
     * <p>Entrada: {@link LembreteRequest#text}.</p>
     * <p>Saída: lista atualizada.</p>
     */
    public ResponseEntity<?> addLembrete(Authentication authentication, @Valid @RequestBody LembreteRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }
        String email = authentication.getName();
        List<String> lembretes = usuarioService.addLembrete(email, request.getText());
        return ResponseEntity.ok(lembretes);
    }

    @PutMapping("/me/lembretes/{index}")
    /**
     * Atualiza um lembrete pelo índice (0..n-1).
     *
     * <p>Entrada: path {@code index} + {@link LembreteRequest#text}.</p>
     * <p>Saída: lista atualizada.</p>
     */
    public ResponseEntity<?> updateLembrete(
            Authentication authentication,
            @PathVariable int index,
            @Valid @RequestBody LembreteRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }
        String email = authentication.getName();
        List<String> lembretes = usuarioService.updateLembrete(email, index, request.getText());
        return ResponseEntity.ok(lembretes);
    }

    @DeleteMapping("/me/lembretes/{index}")
    /**
     * Remove um lembrete pelo índice (0..n-1).
     *
     * <p>Saída: lista atualizada.</p>
     */
    public ResponseEntity<?> deleteLembrete(Authentication authentication, @PathVariable int index) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, tokenCookieService.clearAuthCookie().toString())
                    .body("Não autenticado");
        }
        String email = authentication.getName();
        List<String> lembretes = usuarioService.deleteLembrete(email, index);
        return ResponseEntity.ok(lembretes);
    }
}

