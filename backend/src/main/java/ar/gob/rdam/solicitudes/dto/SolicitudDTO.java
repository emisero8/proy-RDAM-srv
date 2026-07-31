package ar.gob.rdam.solicitudes.dto;

import ar.gob.rdam.domain.enums.EstadoSolicitud;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SolicitudDTO {
    private Long id;
    private String numero;
    private Long ciudadanoId;
    private String ciudadanoNombre;
    private String ciudadanoEmail;
    private String ciudadanoDni;
    private LocalDate ciudadanoFechaNacimiento;
    private String tipoCert;
    private String urgencia;
    private EstadoSolicitud estado;
    private BigDecimal arancel;
    private String observaciones;
    private String motivoRechazo;
    private Long revisorId;
    private String revisorNombre;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<HistorialEstadoDTO> historial;

    @Data
    @Builder
    public static class HistorialEstadoDTO {
        private Long id;
        private EstadoSolicitud estadoAnt;
        private EstadoSolicitud estadoNuevo;
        private String usuarioNombre;
        private String comentario;
        private LocalDateTime createdAt;
    }
}
