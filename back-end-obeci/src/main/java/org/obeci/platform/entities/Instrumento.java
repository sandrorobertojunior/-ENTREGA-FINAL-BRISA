package org.obeci.platform.entities;

import jakarta.persistence.*;
import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "instrumentos", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"turma_id"})
})
@EntityListeners(AuditingEntityListener.class)
/**
 * Entidade JPA que representa um "instrumento" (conjunto de slides) associado a uma turma.
 *
 * <p>O conteúdo dos slides é persistido como string JSON em {@code slidesJson}. O formato
 * do JSON é definido pelo front-end (editor) e não é validado estruturalmente aqui.</p>
 */
public class Instrumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Versão para controle de concorrência (optimistic locking).
     *
     * <p>Quando dois professores editam simultaneamente, esse campo permite detectar
     * atualizações "stale" (um cliente tentando salvar em cima de uma versão antiga)
     * e retornar conflito ao invés de sobrescrever silenciosamente.</p>
     */
    @Version
    // IMPORTANTE: não marcar como NOT NULL no schema via Hibernate.
    // Em bancos já existentes, adicionar uma coluna NOT NULL falha porque linhas antigas
    // começam com NULL. Após o backfill, podemos impor NOT NULL via migração controlada.
    private Long version;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    // JSON dos slides conforme modelo do front-end
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name = "slides_json", nullable = false, columnDefinition = "TEXT")
    private String slidesJson;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dataModificacao = LocalDateTime.now();
}
