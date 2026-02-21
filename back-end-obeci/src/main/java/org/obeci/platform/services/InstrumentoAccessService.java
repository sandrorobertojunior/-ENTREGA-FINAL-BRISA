package org.obeci.platform.services;

import org.obeci.platform.entities.Turma;
import org.obeci.platform.entities.Usuario;
import org.obeci.platform.repositories.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/**
 * Regras de acesso ao Instrumento (1:1 com Turma).
 *
 * <p>Política:
 * <ul>
 *   <li>ADMIN pode acessar qualquer instrumento</li>
 *   <li>caso contrário, somente professores cujo {@code id} esteja em {@code Turma.professorIds}</li>
 * </ul>
 * </p>
 */
@Service
public class InstrumentoAccessService {

    private final TurmaRepository turmaRepository;
    private final UsuarioService usuarioService;

    public InstrumentoAccessService(TurmaRepository turmaRepository, UsuarioService usuarioService) {
        this.turmaRepository = turmaRepository;
        this.usuarioService = usuarioService;
    }

    public void assertCanAccessTurmaInstrumento(Long turmaId, Authentication authentication) {
        if (turmaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "turmaId é obrigatório");
        }
        if (authentication == null || authentication.getName() == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }

        if (hasRole(authentication.getAuthorities(), "ROLE_ADMIN")) {
            return;
        }

        String email = authentication.getName();
        Usuario usuario = usuarioService.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não autorizado"));

        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        if (turma.getProfessorIds() == null || !turma.getProfessorIds().contains(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para acessar este instrumento");
        }
    }

    private static boolean hasRole(Collection<? extends GrantedAuthority> authorities, String expected) {
        if (authorities == null || expected == null) {
            return false;
        }
        return authorities.stream().anyMatch(a -> expected.equalsIgnoreCase(a.getAuthority()));
    }
}
