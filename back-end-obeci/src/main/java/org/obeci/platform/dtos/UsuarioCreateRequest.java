package org.obeci.platform.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO de criação de usuário.
 *
 * <p>Usado no cadastro via {@code POST /auth/register} e também no endpoint admin {@code POST /api/usuarios}.</p>
 * <p>Contém validações mínimas e alguns campos extras vindos do front que não são persistidos atualmente.</p>
 */
public class UsuarioCreateRequest {
    @NotBlank(message = "Nome de usuário é obrigatório")
    private String username; // mapeia dos forms "nome"

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
    private String password; // mapeia dos forms "senha"

    @NotBlank(message = "CPF é obrigatório")
//    @CPF(message = "CPF inválido")
    private String cpf;

    private List<String> arrayRoles; // opcional: ADMIN/PROFESSOR/ALUNO

    // Campos extras presentes em alguns forms (não persistidos atualmente)
    private String documento; // CPF/CNPJ (professor)
    private String escola;    // livre
    private String turma;     // livre

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public List<String> getArrayRoles() { return arrayRoles; }
    public void setArrayRoles(List<String> arrayRoles) { this.arrayRoles = arrayRoles; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getEscola() { return escola; }
    public void setEscola(String escola) { this.escola = escola; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }
}
