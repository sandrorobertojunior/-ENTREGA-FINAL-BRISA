package org.obeci.platform.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Registro de auditoria/colaboração do Instrumento.
 *
 * <p>Objetivo: permitir um "log de alterações" legível por humanos e também servir
 * de trilha de auditoria mínima para depuração de edições concorrentes.</p>
 *
 * <p>Observação de design:
 * <ul>
 *   <li>Não armazenamos o documento inteiro aqui (slidesJson) para não explodir o banco.</li>
 *   <li>Armazenamos metadados (quem/quando) e um resumo (summary) + payload opcional.</li>
 * </ul>
 * </p>
 */
@Data
@Entity
@Table(name = "instrumento_change_logs", indexes = {
        @Index(name = "idx_instrumento_change_logs_turma", columnList = "turma_id"),
        @Index(name = "idx_instrumento_change_logs_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class InstrumentoChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumento_id", nullable = false)
    private Long instrumentoId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    /**
     * Identificador do usuário (email/username) que realizou a ação.
     *
     * <p>Guardamos como string para manter o log estável mesmo se a tabela de usuários mudar.</p>
     */
    @Column(name = "actor", nullable = false)
    private String actor;

    /**
     * Tipo de evento (ex.: SNAPSHOT_UPDATE, IMAGE_UPLOAD, SLIDE_ADD).
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Resumo curto do que mudou, para exibição no painel de log no front-end.
     */
    @Column(name = "summary", nullable = false, length = 300)
    private String summary;

    /**
     * JSON opcional com dados mínimos do evento (não é o documento inteiro).
     * Mantido como TEXT por compatibilidade simples.
     */
    @Lob
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
