package ar.gob.rdam.usuarios.dto;

import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String dniCuil;
    private String telefono;
    private String domicilio;
    private TipoUsuario tipo;
    private RolUsuario rol;
    private Boolean activo;
    private LocalDateTime createdAt;
}
