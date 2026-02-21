package org.obeci.platform.services;

import org.obeci.platform.entities.Usuario;
import org.obeci.platform.repositories.UsuarioRepository;
import org.obeci.platform.dtos.UsuarioCreateRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
/**
 * Camada de serviço para usuários (regras de negócio + persistência).
 *
 * <p>Responsabilidades principais:
 * <ul>
 *   <li>Cadastro (register) com validação de unicidade (email/cpf) e hash de senha.</li>
 *   <li>Login (validação de credenciais).</li>
 *   <li>CRUD administrativo e busca por role.</li>
 *   <li>Integração com Spring Security via {@link UserDetailsService}.</li>
 *   <li>CRUD de lembretes do próprio usuário.</li>
 * </ul>
 * </p>
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>Senha nunca é persistida em texto puro; sempre passa por BCrypt.</li>
 *   <li>Roles são mapeadas para authorities no formato {@code ROLE_*}.</li>
 *   <li>Várias regras sinalizam erro via {@link RuntimeException}, tratada por {@link org.obeci.platform.configs.GlobalExceptionHandler}.</li>
 * </ul>
 * </p>
 */
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        // Observação: o encoder é criado localmente em vez de ser injetado pelo contexto.
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Usuario register(Usuario usuario) {
        // Cadastro de usuário com validações de unicidade e hash da senha.
        // Verifica se já existe email
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new RuntimeException("Email já existe");
        }

        // Verifica se já existe CPF
        if (usuario.getCpf() != null && usuarioRepository.findByCpf(usuario.getCpf()).isPresent()) {
            throw new RuntimeException("CPF já existe");
        }

        // Criptografa a senha e salva
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        // Produção: se não enviar roles, definir uma role padrão (ex.: PROFESSOR)
        if (usuario.getArrayRoles() == null || usuario.getArrayRoles().isEmpty()) {
            usuario.setArrayRoles(java.util.Arrays.asList("PROFESSOR"));
        }
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> login(String email, String senha) {
        // Valida credenciais comparando a senha informada com o hash persistido.
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (passwordEncoder.matches(senha, usuario.getPassword())) {
                return Optional.of(usuario);
            }
        }
        return Optional.empty();
    }

    // Sobrecarga para aceitar DTO de criação de usuário
    public Usuario register(UsuarioCreateRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setCpf(request.getCpf());
        usuario.setArrayRoles(request.getArrayRoles());
        return register(usuario);
    }

    // Método auxiliar para verificar credenciais
    public boolean validateCredentials(String email, String password) {
        return login(email, password).isPresent();
    }

    @Override
    /**
     * Bridge para o Spring Security: carrega usuário por "username".
     *
     * <p>No projeto, {@code username} corresponde ao email.</p>
     *
     * @throws UsernameNotFoundException se não existir usuário com o email informado.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        // Mapeia roles da entidade para authorities do Spring (ROLE_*)
        List<String> authorities = (usuario.getArrayRoles() == null ? java.util.Collections.<String>emptyList() : usuario.getArrayRoles())
                .stream()
                .map(r -> "ROLE_" + r.toUpperCase())
                .collect(Collectors.toList());
        if (authorities.isEmpty()) {
            authorities = java.util.Arrays.asList("ROLE_USER");
        }
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(authorities.toArray(new String[0]))
                .build();
    }

    // CRUD de usuários para administração
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional
    /**
     * Atualiza campos do usuário, aplicando somente valores não vazios.
     *
     * <p>Efeito colateral: persiste alterações no banco.</p>
     */
    public Optional<Usuario> update(Long id, Usuario changes) {
        return usuarioRepository.findById(id).map(existing -> {
            if (changes.getUsername() != null && !changes.getUsername().isBlank()) existing.setUsername(changes.getUsername());
            if (changes.getEmail() != null && !changes.getEmail().isBlank()) existing.setEmail(changes.getEmail());
            if (changes.getCpf() != null && !changes.getCpf().isBlank()) existing.setCpf(changes.getCpf());
            if (changes.getPassword() != null && !changes.getPassword().isBlank()) {
                // Atualização de senha:
                // - nunca persistir senha em texto puro
                // - sempre recriptografar antes de salvar
                existing.setPassword(passwordEncoder.encode(changes.getPassword()));
            }
            if (changes.getArrayRoles() != null && !changes.getArrayRoles().isEmpty()) {
                existing.setArrayRoles(changes.getArrayRoles());
            }
            return usuarioRepository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Busca usuários por role com filtro opcional de texto (username/email)
    public List<Usuario> findByRole(String role, String query) {
        String wanted = role == null ? "" : role.toUpperCase();
        String q = query == null ? "" : query.toLowerCase();
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getArrayRoles() != null &&
                        u.getArrayRoles().stream()
                                .map(r -> r == null ? "" : r.toUpperCase())
                                .anyMatch(r -> r.equals(wanted)))
                .filter(u -> q.isBlank() ||
                        ((u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) ||
                         (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))))
                .collect(Collectors.toList());
    }

    // =====================================================================
    // Lembretes (CRUD) - do próprio usuário autenticado
    // =====================================================================

    @Transactional
    /**
     * Lista lembretes do usuário (cria lista vazia em memória caso nulo).
     *
     * <p>Observação: retorna a lista atual (mutável) da entidade; chamadas subsequentes podem observar alterações.</p>
     */
    public List<String> listLembretes(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (usuario.getLembretes() == null) {
            usuario.setLembretes(new ArrayList<>());
        }
        return usuario.getLembretes();
    }

    @Transactional
    /**
     * Adiciona um lembrete ao final da lista.</p>
     * <p>Normaliza CRLF -> LF e remove espaços das extremidades.</p>
     */
    public List<String> addLembrete(String email, String text) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (usuario.getLembretes() == null) {
            usuario.setLembretes(new ArrayList<>());
        }
        String normalized = normalizeLembrete(text);
        if (normalized.isBlank()) {
            throw new RuntimeException("Lembrete não pode ser vazio");
        }
        usuario.getLembretes().add(normalized);
        usuarioRepository.save(usuario);
        return usuario.getLembretes();
    }

    @Transactional
    /**
     * Atualiza um lembrete pelo índice (0..n-1).</p>
     */
    public List<String> updateLembrete(String email, int index, String text) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (usuario.getLembretes() == null) {
            usuario.setLembretes(new ArrayList<>());
        }
        if (index < 0 || index >= usuario.getLembretes().size()) {
            throw new RuntimeException("Índice de lembrete inválido");
        }
        String normalized = normalizeLembrete(text);
        if (normalized.isBlank()) {
            throw new RuntimeException("Lembrete não pode ser vazio");
        }
        usuario.getLembretes().set(index, normalized);
        usuarioRepository.save(usuario);
        return usuario.getLembretes();
    }

    @Transactional
    /**
     * Remove um lembrete pelo índice (0..n-1).</p>
     */
    public List<String> deleteLembrete(String email, int index) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        if (usuario.getLembretes() == null) {
            usuario.setLembretes(new ArrayList<>());
        }
        if (index < 0 || index >= usuario.getLembretes().size()) {
            throw new RuntimeException("Índice de lembrete inválido");
        }
        usuario.getLembretes().remove(index);
        usuarioRepository.save(usuario);
        return usuario.getLembretes();
    }

    private String normalizeLembrete(String text) {
        // Mantém consistência para armazenamento e renderização no front.
        if (text == null) return "";
        // Normaliza CRLF (Windows) -> LF para manter consistência no storage/render.
        return text.replace("\r\n", "\n").trim();
    }
}

