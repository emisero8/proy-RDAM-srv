package ar.gob.rdam.certificados.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CertificadoDTO {
    private Long id;
    private Long solicitudId;
    private String numeroCertificado;
    private String archivoUrl;
    private String emisorNombre;
    private String firmaDigital;
    private LocalDate fechaVencimiento;
    private LocalDateTime createdAt;
    private Boolean valido;
    private String razon;
}
