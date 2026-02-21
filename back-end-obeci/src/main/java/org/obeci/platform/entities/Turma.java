package org.obeci.platform.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.util.LinkedHashSet;
import java.util.Set;
@Data
@Entity
@Table(name = "turmas")
@EntityListeners(AuditingEntityListener.class)
/**
 * Entidade JPA que representa uma turma.
 *
 * <p>Mapeamento usa IDs primitivos ({@code escolaId} e {@code professorIds}) em vez de
 * relacionamentos JPA (ManyToOne). O vínculo turma-professor é 1:N e é persistido
 * na tabela associativa {@code turma_professores}.</p>
 */
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long escolaId;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(
            name = "turma_professores",
            joinColumns = @JoinColumn(name = "turma_id")
        )
        @Column(name = "professor_id", nullable = false)
        private Set<Long> professorIds = new LinkedHashSet<>();

    @Column(nullable = false)
    private String turno;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Boolean isActive;

    public Turma() {

    }

    public Turma(Long escolaId, Set<Long> professorIds, String turno, String nome, Boolean isActive) {
        this();
        this.escolaId = escolaId;
        if (professorIds != null) {
            this.professorIds = professorIds;
        }
        this.turno = turno;
        this.nome = nome;
        this.isActive = isActive;
    }
}
