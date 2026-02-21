package org.obeci.platform.repositories;

import org.obeci.platform.entities.InstrumentoImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 * Repositório JPA para {@link InstrumentoImage} (armazenamento de imagens em banco).
 */
public interface InstrumentoImageRepository extends JpaRepository<InstrumentoImage, Long> {
}
