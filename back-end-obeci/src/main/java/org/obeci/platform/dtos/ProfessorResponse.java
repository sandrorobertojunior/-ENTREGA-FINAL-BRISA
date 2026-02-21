package org.obeci.platform.dtos;

/**
 * DTO de resposta resumida para professores.
 *
 * <p>Usado em {@code GET /api/usuarios/professores} para evitar exposição de campos sensíveis (ex.: senha).</p>
 */
public class ProfessorResponse {
    private Long id;
    private String username;
    private String email;

    public ProfessorResponse() {}

    public ProfessorResponse(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
