package org.obeci.platform.repositories;

import org.obeci.platform.entities.InstrumentoChangeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Repositório do log de alterações do instrumento.
 */
public interface InstrumentoChangeLogRepository extends JpaRepository<InstrumentoChangeLog, Long> {

    List<InstrumentoChangeLog> findByTurmaIdOrderByCreatedAtDesc(Long turmaId, Pageable pageable);
}
