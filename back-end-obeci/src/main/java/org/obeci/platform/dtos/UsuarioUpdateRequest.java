package org.obeci.platform.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO de atualização de usuário (admin).
 *
 * <p>Usado em {@code PUT /api/usuarios/{id}}. Campos são opcionais; se enviados, serão aplicados no service.</p>
 */
public class UsuarioUpdateRequest {
    // Opcional para atualização: se enviado, será aplicado
    private String username;

    @Email(message = "Email inválido")
    private String email;
    
    private String cpf; // opcional para update

    private List<String> arrayRoles;

    // Senha opcional para atualização.
    // Se enviada, será re-codificada no service.
    // Mantém a mesma regra mínima usada no update do próprio usuário (/auth/me).
    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public List<String> getArrayRoles() { return arrayRoles; }
    public void setArrayRoles(List<String> arrayRoles) { this.arrayRoles = arrayRoles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
