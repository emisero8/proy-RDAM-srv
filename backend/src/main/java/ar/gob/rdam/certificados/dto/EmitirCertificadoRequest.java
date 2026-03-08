package ar.gob.rdam.certificados.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmitirCertificadoRequest {
    @NotNull
    private Long solicitudId;
    private String comentario;
}
