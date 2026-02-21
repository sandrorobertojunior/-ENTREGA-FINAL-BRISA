package org.obeci.platform.controllers;

import org.obeci.platform.entities.Turma;
import org.obeci.platform.dtos.TurmaCreateRequest;
import org.obeci.platform.dtos.TurmaUpdateRequest;
import jakarta.validation.Valid;
import org.obeci.platform.services.TurmaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Collections;
import org.obeci.platform.services.UsuarioService;

import java.util.LinkedHashSet;
import java.util.Set;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/turmas")
/**
 * Controller REST para gestão de turmas.
 *
 * <p>Expõe CRUD e consultas; inclui endpoint especial {@code /mine} que aplica
 * regras de visibilidade com base nas roles do usuário autenticado.</p>
 */
public class TurmaController {

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    /**
     * Lista todas as turmas.
     */
    public ResponseEntity<List<Turma>> getAllTurmas() {
        List<Turma> turmas = turmaService.getAllTurmas();
        return ResponseEntity.ok(turmas);
    }

    // Retorna turmas visíveis ao usuário atual: ADMIN vê todas; PROFESSOR vê as suas.
    @GetMapping("/mine")
    /**
     * Retorna turmas visíveis ao usuário autenticado.
     *
     * <p>Regras:
     * <ul>
     *   <li>ADMIN: retorna todas as turmas.</li>
    *   <li>PROFESSOR: retorna apenas turmas onde {@code professorIds} contém o id do usuário.</li>
     *   <li>Outras roles: retorna lista vazia.</li>
     * </ul>
     * </p>
     */
    public ResponseEntity<List<Turma>> getMyTurmas(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Collections.<Turma>emptyList());
        }
        String email = authentication.getName();
        return usuarioService.findByEmail(email)
            .map(u -> {
                List<String> roles = u.getArrayRoles() == null ? java.util.Collections.emptyList() : u.getArrayRoles();
                boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r));
                if (isAdmin) {
                    return ResponseEntity.ok(turmaService.getAllTurmas());
                }
                boolean isProfessor = roles.stream().anyMatch(r -> "PROFESSOR".equalsIgnoreCase(r));
                if (isProfessor) {
                    return ResponseEntity.ok(turmaService.getTurmasByProfessorId(u.getId()));
                }
                return ResponseEntity.ok(Collections.<Turma>emptyList());
            })
            .orElseGet(() -> ResponseEntity.status(404).body(Collections.<Turma>emptyList()));
    }

    @GetMapping("/{id}")
    /**
     * Obtém turma por id.
     *
     * <p>Saída: {@code Optional<Turma>} (o controller não transforma ausência em 404).</p>
     */
    public ResponseEntity<Optional<Turma>> getTurmaById(@PathVariable Long id) {
        Optional<Turma> turma = turmaService.getTurmaById(id);
        return ResponseEntity.ok(turma);
    }

    @PostMapping
    /**
     * Cria turma a partir de {@link TurmaCreateRequest}.</p>
     */
    public ResponseEntity<Turma> createTurma(@Valid @RequestBody TurmaCreateRequest request) {
        Turma turma = new Turma();
        turma.setEscolaId(request.getEscolaId());
        Set<Long> professorIds = request.getProfessorIds() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(request.getProfessorIds());
        turma.setProfessorIds(professorIds);
        turma.setTurno(request.getTurno());
        turma.setNome(request.getNome());
        turma.setIsActive(request.getIsActive());
        Turma createdTurma = turmaService.createTurma(turma);
        return ResponseEntity.ok(createdTurma);
    }

    @PutMapping("/{id}")
    /**
     * Atualiza uma turma.</p>
     */
    public ResponseEntity<Turma> updateTurma(@PathVariable Long id, @Valid @RequestBody TurmaUpdateRequest request) {
        Turma turmaDetails = new Turma();
        turmaDetails.setEscolaId(request.getEscolaId());
        Set<Long> professorIds = request.getProfessorIds() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(request.getProfessorIds());
        turmaDetails.setProfessorIds(professorIds);
        turmaDetails.setTurno(request.getTurno());
        turmaDetails.setNome(request.getNome());
        turmaDetails.setIsActive(request.getIsActive());
        Turma updatedTurma = turmaService.updateTurma(id, turmaDetails);
        if (updatedTurma != null) {
            return ResponseEntity.ok(updatedTurma);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    /**
     * Remove turma por id.</p>
     */
    public ResponseEntity<Boolean> deleteTurma(@PathVariable Long id) {
        boolean deleted = turmaService.deleteTurma(id);
        if (deleted) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/escola/{escolaId}")
    /**
     * Lista turmas por escola.</p>
     */
    public ResponseEntity<List<Turma>> getTurmasByEscolaId(@PathVariable Long escolaId) {
        List<Turma> turmas = turmaService.getTurmasByEscolaId(escolaId);
        return ResponseEntity.ok(turmas);
    }

    @GetMapping("/professor/{professorId}")
    /**
     * Lista turmas por professor.</p>
     */
    public ResponseEntity<List<Turma>> getTurmasByProfessorId(@PathVariable Long professorId) {
        List<Turma> turmas = turmaService.getTurmasByProfessorId(professorId);
        return ResponseEntity.ok(turmas);
    }
}