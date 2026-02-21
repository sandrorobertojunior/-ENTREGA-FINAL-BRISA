package org.obeci.platform.services;

import org.obeci.platform.entities.Turma;
import org.obeci.platform.repositories.TurmaRepository;
import org.obeci.platform.exceptions.DuplicateTurmaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
/**
 * Camada de serviço para turmas.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>CRUD de {@link Turma}.</li>
 *   <li>Aplicar regra de unicidade de nome por escola.</li>
 *   <li>Garantir criação de instrumento vazio ao criar turma.</li>
 * </ul>
 * </p>
 */
public class TurmaService {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private InstrumentoService instrumentoService;

    public List<Turma> getAllTurmas() {
        return turmaRepository.findAll();
    }

    public Optional<Turma> getTurmaById(Long id) {
        return turmaRepository.findById(id);
    }

    public Turma createTurma(Turma turma) {
        // Regra de negócio aplicada antes de persistir.
        // Regra de negócio: não permitir duas turmas com o mesmo nome dentro da mesma escola.
        // Normaliza com trim() e compara sem diferenciar maiúsculas/minúsculas.
        String nome = turma.getNome() == null ? null : turma.getNome().trim();
        turma.setNome(nome);
        if (nome != null && turma.getEscolaId() != null
                && turmaRepository.existsByEscolaIdAndNomeIgnoreCase(turma.getEscolaId(), nome)) {
            throw new DuplicateTurmaException("Já existe uma turma com esse nome nesta escola");
        }

        // Normaliza professorIds (sem nulls/duplicados)
        Set<Long> professorIds = turma.getProfessorIds() == null
            ? new LinkedHashSet<>()
            : new LinkedHashSet<>(turma.getProfessorIds());
        professorIds.removeIf(id -> id == null || id <= 0);
        turma.setProfessorIds(professorIds);

        Turma saved = turmaRepository.save(turma);
        // cria automaticamente um Instrumento vazio para a turma.
        // O erro é ignorado (fail-safe) para não impedir a criação da turma.
        try {
            instrumentoService.createEmptyForTurma(saved.getId());
        } catch (Exception ignored) {
        }
        return saved;
    }

    public Turma updateTurma(Long id, Turma turmaDetails) {
        // Atualização com a mesma validação de unicidade do create.
        Turma turma = turmaRepository.findById(id).orElse(null);
        if (turma != null) {
            // Mesma regra do create, excluindo o próprio id para permitir atualização sem "auto-duplicar".
            String nome = turmaDetails.getNome() == null ? null : turmaDetails.getNome().trim();
            turmaDetails.setNome(nome);
            if (nome != null && turmaDetails.getEscolaId() != null
                    && turmaRepository.existsByEscolaIdAndNomeIgnoreCaseAndIdNot(turmaDetails.getEscolaId(), nome, id)) {
                throw new DuplicateTurmaException("Já existe uma turma com esse nome nesta escola");
            }

            turma.setEscolaId(turmaDetails.getEscolaId());
            Set<Long> professorIds = turmaDetails.getProfessorIds() == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(turmaDetails.getProfessorIds());
            professorIds.removeIf(pid -> pid == null || pid <= 0);
            turma.setProfessorIds(professorIds);
            turma.setTurno(turmaDetails.getTurno());
            turma.setNome(turmaDetails.getNome());
            turma.setIsActive(turmaDetails.getIsActive());
            return turmaRepository.save(turma);
        }
        return null;
    }

    public boolean deleteTurma(Long id) {
        if (turmaRepository.existsById(id)) {
            turmaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Turma> getTurmasByEscolaId(Long escolaId) {
        return turmaRepository.findByEscolaId(escolaId);
    }

    public List<Turma> getTurmasByProfessorId(Long professorId) {
        return turmaRepository.findByProfessorId(professorId);
    }

    public List<Turma> getActiveTurmas(Boolean isActive) {
        return turmaRepository.findByIsActive(isActive);
    }
}