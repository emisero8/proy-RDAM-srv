package ar.gob.rdam.pagos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IniciarPagoRequest {
    @NotNull(message = "El ID de solicitud es obligatorio")
    private Long solicitudId;
}
