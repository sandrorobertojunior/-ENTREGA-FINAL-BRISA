package org.obeci.platform.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
/**
 * Entidade JPA que representa um usuário do sistema.
 *
 * <p>Mapeamento:
 * <ul>
 *   <li>Tabela: {@code usuarios}</li>
 *   <li>Campos principais: username, email (único), cpf (único), password (hash).</li>
 *   <li>{@code array_roles}: roles do usuário como {@code text[]} (PostgreSQL).</li>
 *   <li>{@code lembretes}: lista de lembretes do usuário como {@code text[]} (PostgreSQL).</li>
 * </ul>
 * </p>
 *
 * <p>Pontos críticos:
 * <ul>
 *   <li>{@code password} deve ser persistida como hash (ver {@link org.obeci.platform.services.UsuarioService}).</li>
 *   <li>{@code lembretes} pode conter múltiplas linhas (\n) por item.</li>
 * </ul>
 * </p>
 */
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String password;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime dataModificacao = LocalDateTime.now();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "array_roles", columnDefinition = "text[]")
    private List<String> arrayRoles = new ArrayList<>();

    // Lembretes do usuário (persistidos no PostgreSQL como text[])
    // Cada item pode conter múltiplas linhas (\n).
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "lembretes", columnDefinition = "text[]")
    private List<String> lembretes = new ArrayList<>();

    public Usuario() {

    }

    public Usuario(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
    }
}