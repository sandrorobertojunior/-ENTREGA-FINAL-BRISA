package org.obeci.platform.entities;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Data
@Entity
@Table(name = "escolas")
@EntityListeners(AuditingEntityListener.class)
/**
 * Entidade JPA que representa uma escola.
 *
 * <p>Campos: nome e flag {@code isActive} (ativação/desativação lógica).</p>
 */
public class Escola {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Boolean isActive;

    public Escola() {

    }

    public Escola(String nome, Boolean isActive) {
        this();
        this.nome = nome;
        this.isActive = isActive;
    }
}
