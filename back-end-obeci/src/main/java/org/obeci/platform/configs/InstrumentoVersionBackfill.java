package org.obeci.platform.configs;

import org.obeci.platform.repositories.InstrumentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Backfill de version para optimistic locking.
 *
 * <p>Motivo: em bancos já existentes (Postgres), adicionar uma coluna {@code version}
 * como NOT NULL falha, porque linhas antigas ficam com NULL. Mantemos o campo
 * version como nullable no mapeamento e fazemos um backfill seguro no startup.</p>
 *
 * <p>Depois que todas as linhas estiverem com valor, você pode (opcionalmente)
 * impor NOT NULL via uma migração controlada (Flyway/Liquibase/SQL manual).</p>
 */
@Component
@Profile("!test")
public class InstrumentoVersionBackfill {

    private static final Logger log = LoggerFactory.getLogger(InstrumentoVersionBackfill.class);

    private final InstrumentoRepository instrumentoRepository;
    private final TransactionTemplate transactionTemplate;

    public InstrumentoVersionBackfill(
            InstrumentoRepository instrumentoRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.instrumentoRepository = instrumentoRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        try {
            int updated = transactionTemplate.execute(status -> instrumentoRepository.backfillNullVersions());
            if (updated > 0) {
                log.info("Backfill Instrumento.version: {} linhas atualizadas para 0", updated);
            }
        } catch (Exception e) {
            // Não derrubar a aplicação por causa do backfill; mas deixar visível.
            log.warn("Falha no backfill de Instrumento.version (otimista). Motivo: {}", e.getMessage());
        }
    }
}
