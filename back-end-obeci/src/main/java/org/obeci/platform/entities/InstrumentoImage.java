package org.obeci.platform.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "instrumento_images")
@EntityListeners(AuditingEntityListener.class)
/**
 * Entidade JPA para armazenar imagens enviadas pelo editor de instrumento.
 *
 * <p>Armazena bytes em {@code data} (LOB) e metadados como {@code contentType} e {@code originalName}.</p>
 */
public class InstrumentoImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "data", nullable = false)
    private byte[] data;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "original_name")
    private String originalName;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();
}
