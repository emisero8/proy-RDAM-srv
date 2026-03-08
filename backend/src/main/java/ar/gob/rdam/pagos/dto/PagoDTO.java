package ar.gob.rdam.pagos.dto;

import ar.gob.rdam.domain.enums.EstadoPago;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PagoDTO {
    private Long id;
    private Long solicitudId;
    private BigDecimal monto;
    private EstadoPago estado;
    private LocalDateTime createdAt;

    /** ID de transacción en PlusPagos (se registra tras el webhook) */
    private String referencia;

    // ─── Campos para construir el formulario POST hacia PlusPagos ──────────────
    /** URL base del mock/pasarela (ej: http://localhost:3000) */
    private String plusPagosUrl;

    /** GUID del comercio (test-merchant-001) */
    private String comercio;

    /** ID de transacción generado por RDAM (TXN-SOL-YYYY-NNN-timestamp) */
    private String transaccionComercioId;

    // Campos encriptados con AES-256-CBC (listos para meter en inputs hidden)
    private String montoEnc;
    private String callbackSuccessEnc;
    private String callbackCancelEnc;
    private String urlSuccessEnc;
    private String urlErrorEnc;
    private String informacionEnc;
}
