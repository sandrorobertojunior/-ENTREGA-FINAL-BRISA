package org.obeci.platform.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de transporte do instrumento associado a uma turma.
 *
 * <p>Usado nos endpoints de {@code /api/instrumentos/*} para retornar o JSON dos slides.</p>
 */
@Data
@AllArgsConstructor
public class InstrumentoDto {
    private Long id;
    private Long turmaId;
    private String slidesJson;
    /**
     * Versão do instrumento (optimistic locking). Usada pelo modo colaborativo para
     * evitar sobrescrita silenciosa em edições simultâneas.
     */
    private Long version;
}
