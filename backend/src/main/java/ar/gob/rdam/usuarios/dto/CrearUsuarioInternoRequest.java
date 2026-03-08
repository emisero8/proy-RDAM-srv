package ar.gob.rdam.usuarios.dto;

import ar.gob.rdam.domain.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearUsuarioInternoRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    private String dniCuil;
    private String telefono;

    @NotNull
    private RolUsuario rol; // GESTOR o ADMIN
}
