package ar.gob.rdam.usuarios.repository;

import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

        Optional<Usuario> findByEmail(String email);

        Optional<Usuario> findByDniCuil(String dniCuil);

        boolean existsByEmail(String email);

        boolean existsByDniCuil(String dniCuil);

        @Query("""
                        SELECT u FROM Usuario u
                        WHERE u.tipo = ar.gob.rdam.domain.enums.TipoUsuario.INTERNO
                          AND (:activo = -1 OR (CASE WHEN u.activo = true THEN 1 ELSE 0 END) = :activo)
                          AND (:search = '' OR LOWER(u.nombre) LIKE CONCAT('%',:search,'%')
                               OR LOWER(u.apellido) LIKE CONCAT('%',:search,'%')
                               OR LOWER(u.email) LIKE CONCAT('%',:search,'%'))
                        """)
        Page<Usuario> buscarUsuariosSinRol(
                        @Param("activo") int activo,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("""
                        SELECT u FROM Usuario u
                        WHERE u.tipo = ar.gob.rdam.domain.enums.TipoUsuario.INTERNO
                          AND u.rol = :rol
                          AND (:activo = -1 OR (CASE WHEN u.activo = true THEN 1 ELSE 0 END) = :activo)
                          AND (:search = '' OR LOWER(u.nombre) LIKE CONCAT('%',:search,'%')
                               OR LOWER(u.apellido) LIKE CONCAT('%',:search,'%')
                               OR LOWER(u.email) LIKE CONCAT('%',:search,'%'))
                        """)
        Page<Usuario> buscarUsuariosConRol(
                        @Param("rol") RolUsuario rol,
                        @Param("activo") int activo,
                        @Param("search") String search,
                        Pageable pageable);
}
