package org.obeci.platform.repositories;

import org.obeci.platform.entities.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
/**
 * Repositório JPA para {@link Turma}.
 *
 * <p>Inclui métodos derivados e verificações de duplicidade de nome por escola.</p>
 */
public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findByEscolaId(Long escolaId);

    @Query("select distinct t from Turma t join t.professorIds p where p = :professorId")
    List<Turma> findByProfessorId(@Param("professorId") Long professorId);

    List<Turma> findByIsActive(Boolean isActive);

    // Verifica se já existe uma turma com o mesmo nome (case-insensitive) na mesma escola.
    boolean existsByEscolaIdAndNomeIgnoreCase(Long escolaId, String nome);

    // Usado no update: verifica duplicidade excluindo o próprio registro (id atual).
    boolean existsByEscolaIdAndNomeIgnoreCaseAndIdNot(Long escolaId, String nome, Long id);
}