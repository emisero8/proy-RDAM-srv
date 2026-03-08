package ar.gob.rdam.reportes.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

@Data
@Builder
public class ResumenDTO {
    private String periodo;
    private SolicitudesResumen solicitudes;
    private double tiempoPromedioResolucionHs;
    private BigDecimal ingresosMes;

    @Data
    @Builder
    public static class SolicitudesResumen {
        private long total;
        private long pendienteRevision;
        private long enRevision;
        private long aprobadas;
        private long rechazadas;
        private long pendientePago;
        private long pagadas;
        private long emitidas;
        private long expiradas;
    }
}
