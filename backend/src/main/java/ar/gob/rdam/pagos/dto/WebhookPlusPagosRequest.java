package ar.gob.rdam.pagos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Payload que envía el mock de PlusPagos al webhook del backend.
 *
 * Formato real del mock:
 * {
 *   "Tipo":                    "PAGO",
 *   "TransaccionPlataformaId": "123456",
 *   "TransaccionComercioId":   "TXN-SOL-2026-001-...",
 *   "Monto":                   "1500.00",
 *   "EstadoId":                "3",
 *   "Estado":                  "REALIZADA",
 *   "FechaProcesamiento":      "2026-03-04T17:00:00.000Z"
 * }
 *
 * EstadoId: 3 = REALIZADA (aprobado), 4 = RECHAZADA
 */
@Data
public class WebhookPlusPagosRequest {

    @JsonProperty("Tipo")
    private String tipo;

    @JsonProperty("TransaccionPlataformaId")
    private String transaccionPlataformaId;

    /** Contiene el transaccionComercioId generado por el backend, formato: TXN-SOL-YYYY-NNN-timestamp */
    @JsonProperty("TransaccionComercioId")
    private String transaccionComercioId;

    @JsonProperty("Monto")
    private String monto;

    @JsonProperty("EstadoId")
    private String estadoId;

    /** "REALIZADA" = aprobado, "RECHAZADA" = rechazado */
    @JsonProperty("Estado")
    private String estado;

    @JsonProperty("FechaProcesamiento")
    private String fechaProcesamiento;
}
