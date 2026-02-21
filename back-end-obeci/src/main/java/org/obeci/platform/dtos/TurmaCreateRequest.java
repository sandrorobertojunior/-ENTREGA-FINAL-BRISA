package org.obeci.platform.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO de criação de turma.
 *
 * <p>Usado em {@code POST /api/turmas}.</p>
 */
public class TurmaCreateRequest {
    @NotNull(message = "escolaId é obrigatório")
    private Long escolaId;

    @NotEmpty(message = "professorIds é obrigatório")
    private List<Long> professorIds;

    @NotBlank(message = "Turno é obrigatório")
    private String turno;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "isActive é obrigatório")
    private Boolean isActive;

    public Long getEscolaId() { return escolaId; }
    public void setEscolaId(Long escolaId) { this.escolaId = escolaId; }

    public List<Long> getProfessorIds() { return professorIds; }
    public void setProfessorIds(List<Long> professorIds) { this.professorIds = professorIds; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
