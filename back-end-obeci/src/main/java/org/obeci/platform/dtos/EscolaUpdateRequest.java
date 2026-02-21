package org.obeci.platform.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de atualização de escola.
 *
 * <p>Usado em {@code PUT /api/escolas/{id}}.</p>
 */
public class EscolaUpdateRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "isActive é obrigatório")
    private Boolean isActive;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
