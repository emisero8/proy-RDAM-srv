package ar.gob.rdam.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RechazarSolicitudRequest {

    @NotBlank(message = "El motivo de rechazo es obligatorio")
    private String motivoRechazo;

    private String comentario;
}
