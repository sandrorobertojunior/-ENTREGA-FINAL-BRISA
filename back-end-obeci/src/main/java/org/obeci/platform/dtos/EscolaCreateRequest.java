package org.obeci.platform.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de criação de escola.
 *
 * <p>Usado em {@code POST /api/escolas}.</p>
 * <p>Observação: o campo {@code cidade} é validado, mas não é persistido na entidade atual.</p>
 */
public class EscolaCreateRequest {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    // Campo presente no formulário; atualmente não é persistido no modelo.
    // Mantido aqui para validação e futura evolução do domínio.
    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;

    @NotNull(message = "isActive é obrigatório")
    private Boolean isActive;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
