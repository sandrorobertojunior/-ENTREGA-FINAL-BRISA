package org.obeci.platform.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO de atualização do próprio usuário (self-service).
 *
 * <p>Usado em {@code PUT /auth/me}. Campos são opcionais e aplicados condicionalmente.</p>
 */
public class UsuarioSelfUpdateRequest {
    private String username;

    @Email(message = "Email inválido")
    private String email;

    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String password;

    private String cpf; // opcional, normalmente não alterado pelo próprio usuário

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}
