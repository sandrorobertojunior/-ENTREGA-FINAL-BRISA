package org.obeci.platform.repositories;

import org.obeci.platform.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório JPA para {@link Usuario}.
 *
 * <p>Inclui queries derivadas (findBy...) e consultas nativas para filtros em arrays PostgreSQL.</p>
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por email (para login)
    Optional<Usuario> findByEmail(String email);

    // Buscar por username
    Optional<Usuario> findByUsername(String username);

    // Verificar se email existe
    boolean existsByEmail(String email);

    // Verificar se username existe
    boolean existsByUsername(String username);

    // Buscar por CPF
    Optional<Usuario> findByCpf(String cpf);

    // Verificar se CPF existe
    boolean existsByCpf(String cpf);

    // Buscar usuários por role (usando PostgreSQL array)
    @Query(value = "SELECT * FROM usuarios WHERE :role = ANY(array_roles)", nativeQuery = true)
    /**
     * Lista usuários que possuem a role informada em {@code array_roles} (PostgreSQL {@code ANY}).
     */
    List<Usuario> findByRole(@Param("role") String role);

    // Buscar usuários que tenham qualquer uma das roles
    @Query(value = "SELECT * FROM usuarios WHERE array_roles && ARRAY[:roles]", nativeQuery = true)
    /**
     * Lista usuários que possuam interseção entre {@code array_roles} e a lista informada ({@code &&}).
     */
    List<Usuario> findByAnyRole(@Param("roles") List<String> roles);
}