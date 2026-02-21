package org.obeci.platform.dtos.collab;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO de leitura do log de alterações.
 *
 * <p>Usado tanto por REST (carregar histórico) quanto por WebSocket (push em tempo real).</p>
 */
@Data
@AllArgsConstructor
public class InstrumentoChangeLogDto {
    private Long id;
    private Long instrumentoId;
    private Long turmaId;
    private String actor;
    private String eventType;
    private String summary;
    private String payloadJson;
    private LocalDateTime createdAt;
}
