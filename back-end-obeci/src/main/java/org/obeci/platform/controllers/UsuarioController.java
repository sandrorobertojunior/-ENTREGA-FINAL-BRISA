package org.obeci.platform.controllers;

import org.obeci.platform.entities.Usuario;
import org.obeci.platform.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import org.obeci.platform.dtos.UsuarioCreateRequest;
import org.obeci.platform.dtos.UsuarioUpdateRequest;
import org.obeci.platform.dtos.ProfessorResponse;

@RestController
@RequestMapping("/api/usuarios")
/**
 * Controller REST para administração de usuários.
 *
 * <p>Observação: as permissões (ADMIN) são definidas em {@link org.obeci.platform.configs.SecurityConfiguration}.</p>
 */
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Lista todos os usuários (ADMIN)
    @GetMapping
    /**
     * Lista todos os usuários.</p>
     */
    public ResponseEntity<List<Usuario>> findAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    // Obtém um usuário por ID (ADMIN)
    @GetMapping("/{id}")
    /**
     * Obtém um usuário por id.</p>
     * <p>Saída: {@code Optional<Usuario>} (o controller não transforma ausência em 404).</p>
     */
    public ResponseEntity<Optional<Usuario>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    // Cria usuário (ADMIN) — utiliza mesma regra do register, mas sob /api/usuarios
    @PostMapping
    /**
     * Cria um usuário (admin), reutilizando regras de {@link UsuarioService#register(Usuario)}.</p>
     */
    public ResponseEntity<Usuario> create(@Valid @RequestBody UsuarioCreateRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(request.getPassword());
        usuario.setCpf(request.getCpf());
        usuario.setArrayRoles(request.getArrayRoles());
        Usuario created = usuarioService.register(usuario);
        return ResponseEntity.ok(created);
    }

    // Atualiza usuário (ADMIN)
    @PutMapping("/{id}")
    /**
     * Atualiza um usuário por id.</p>
     */
    public ResponseEntity<Optional<Usuario>> update(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        Usuario usuario = new Usuario();
        usuario.setUsername(request.getUsername());
        usuario.setEmail(request.getEmail());
        usuario.setCpf(request.getCpf());
        usuario.setArrayRoles(request.getArrayRoles());
        usuario.setPassword(request.getPassword());
        Optional<Usuario> updated = usuarioService.update(id, usuario);
        if (updated.isPresent()) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    // Exclui usuário (ADMIN)
    @DeleteMapping("/{id}")
    /**
     * Exclui um usuário por id.</p>
     */
    public ResponseEntity<Boolean> delete(@PathVariable Long id) {
        boolean deleted = usuarioService.delete(id);
        if (deleted) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.notFound().build();
    }

    // Lista usuários por role com busca opcional (ADMIN)
    @GetMapping("/role/{role}")
    /**
     * Lista usuários filtrando por role, com busca opcional por termo {@code q}.</p>
     */
    public ResponseEntity<List<Usuario>> findByRole(
            @PathVariable String role,
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(usuarioService.findByRole(role, q));
    }

    // Lista todos os professores (ADMIN) em formato resumido (sem senha)
    @GetMapping("/professores")
    /**
     * Lista professores em formato resumido ({@link ProfessorResponse}).</p>
     */
    public ResponseEntity<List<ProfessorResponse>> listProfessores() {
        List<Usuario> professores = usuarioService.findByRole("PROFESSOR", null);
        List<ProfessorResponse> result = professores.stream()
                .map(u -> new ProfessorResponse(u.getId(), u.getUsername(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(result);
    }
}
