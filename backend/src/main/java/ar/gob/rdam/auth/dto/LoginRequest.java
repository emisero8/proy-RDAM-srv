package ar.gob.rdam.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El identificador es obligatorio")
    private String identificador;  // acepta email o CUIL

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
