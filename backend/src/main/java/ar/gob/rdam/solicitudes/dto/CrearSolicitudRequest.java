package ar.gob.rdam.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearSolicitudRequest {

    @NotBlank
    private String tipoCert; // ej: LIBRE_DEUDA

    private String urgencia = "NORMAL"; // NORMAL | URGENTE

    private String observaciones;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    private String dni;

    private java.time.LocalDate fechaNacimiento;
}
