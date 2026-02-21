package org.obeci.platform.repositories;

import org.obeci.platform.entities.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
/**
 * Repositório JPA para {@link Escola}.
 */
public interface EscolaRepository extends JpaRepository<Escola, Long> {
    List<Escola> findByIsActive(Boolean isActive);
    List<Escola> findByNomeContainingIgnoreCase(String nome);
}