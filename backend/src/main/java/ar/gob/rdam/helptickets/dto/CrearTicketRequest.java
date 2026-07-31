package ar.gob.rdam.helptickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrearTicketRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 256, message = "El mensaje no puede superar los 256 caracteres")
    private String mensaje;
}
