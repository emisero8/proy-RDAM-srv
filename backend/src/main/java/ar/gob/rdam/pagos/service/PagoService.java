package ar.gob.rdam.pagos.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.Pago;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.enums.EstadoPago;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.pagos.dto.IniciarPagoRequest;
import ar.gob.rdam.pagos.dto.PagoDTO;
import ar.gob.rdam.pagos.dto.WebhookPlusPagosRequest;
import ar.gob.rdam.pagos.repository.PagoRepository;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import ar.gob.rdam.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final PagoRepository pagoRepository;
    private final SolicitudRepository solicitudRepository;
    private final SolicitudService solicitudService;
    private final PlusPagosEncryptionService encryptionService;

    @Value("${rdam.pluspagos.api-url}")
    private String plusPagosApiUrl;

    @Value("${rdam.pluspagos.merchant-guid}")
    private String merchantGuid;

    @Value("${rdam.pluspagos.secret-key}")
    private String secretKey;

    @Value("${rdam.pluspagos.frontend-url}")
    private String frontendUrl;

    // ─── Iniciar pago (ciudadano) ─────────────────────────────────────────────

    @Transactional
    public PagoDTO iniciarPago(IniciarPagoRequest request) {
        Solicitud solicitud = solicitudRepository.findById(request.getSolicitudId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Solicitud no encontrada."));

        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE_PAGO) {
            throw new BusinessException("BUSINESS_RULE",
                    "La solicitud no está en estado PENDIENTE_PAGO.");
        }

        // Si ya existe un pago PENDIENTE, lo reutilizamos (reintento luego de webhook
        // no recibido)
        // Si ya existe un pago APROBADO, bloqueamos (no duplicar pago exitoso)
        Pago pagoExistente = pagoRepository.findBySolicitudId(solicitud.getId()).orElse(null);
        if (pagoExistente != null && pagoExistente.getEstado() == EstadoPago.APROBADO) {
            throw new BusinessException("CONFLICT", "Esta solicitud ya fue pagada correctamente.");
        }

        // Generar ID de transacción único: TXN-<numero_solicitud>-<timestamp>
        String transaccionComercioId = "TXN-" + solicitud.getNumero() + "-" + System.currentTimeMillis();

        // Monto en centavos (PlusPagos trabaja en centavos)
        long montoCentavos = solicitud.getArancel()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // URLs de callback (backend recibe notificación del mock)
        String backendBase = "http://localhost:8080/v1";
        String callbackOk = backendBase + "/pagos/webhook/callback?txn=" + transaccionComercioId + "&status=success";
        String callbackCancel = backendBase + "/pagos/webhook/callback?txn=" + transaccionComercioId + "&status=cancel";

        // URLs de redirección del usuario — incluye solicitudId para navegar de vuelta
        String solId = solicitud.getId().toString();
        String urlSuccess = frontendUrl + "/pago/resultado?status=success&txn=" + transaccionComercioId + "&sol="
                + solId;
        String urlError = frontendUrl + "/pago/resultado?status=error&txn=" + transaccionComercioId + "&sol=" + solId;

        // Información adicional (encriptada)
        String informacion = "{\"solicitudId\":" + solicitud.getId()
                + ",\"numero\":\"" + solicitud.getNumero() + "\"}";

        // Encriptar campos sensibles
        String secret = secretKey;
        String montoEnc = encryptionService.encrypt(String.valueOf(montoCentavos), secret);
        String callbackSuccessEnc = encryptionService.encrypt(callbackOk, secret);
        String callbackCancelEnc = encryptionService.encrypt(callbackCancel, secret);
        String urlSuccessEnc = encryptionService.encrypt(urlSuccess, secret);
        String urlErrorEnc = encryptionService.encrypt(urlError, secret);
        String informacionEnc = encryptionService.encrypt(informacion, secret);

        // Guardar pago: reutilizar registro existente (reintento) o crear uno nuevo
        Pago pago;
        if (pagoExistente != null) {
            pagoExistente.setEstado(EstadoPago.PENDIENTE);
            pagoExistente.setReferencia(null);
            pago = pagoRepository.save(pagoExistente);
            log.info("Pago reutilizado para solicitud {}: txn={}", solicitud.getNumero(), transaccionComercioId);
        } else {
            pago = pagoRepository.save(Pago.builder()
                    .solicitud(solicitud)
                    .monto(solicitud.getArancel())
                    .checkoutUrl(plusPagosApiUrl)
                    .estado(EstadoPago.PENDIENTE)
                    .build());
            log.info("Pago iniciado para solicitud {}: monto={} centavos, txn={}",
                    solicitud.getNumero(), montoCentavos, transaccionComercioId);
        }

        return PagoDTO.builder()
                .id(pago.getId())
                .solicitudId(solicitud.getId())
                .monto(pago.getMonto())
                .estado(pago.getEstado())
                .createdAt(pago.getCreatedAt())
                // Datos para que el frontend construya el POST a PlusPagos
                .plusPagosUrl(plusPagosApiUrl)
                .comercio(merchantGuid)
                .transaccionComercioId(transaccionComercioId)
                .montoEnc(montoEnc)
                .callbackSuccessEnc(callbackSuccessEnc)
                .callbackCancelEnc(callbackCancelEnc)
                .urlSuccessEnc(urlSuccessEnc)
                .urlErrorEnc(urlErrorEnc)
                .informacionEnc(informacionEnc)
                .build();
    }

    // ─── Webhook PlusPagos ─────────────────────────────────────────────────────

    @Transactional
    public void procesarWebhook(WebhookPlusPagosRequest payload) {
        log.info("Webhook PlusPagos recibido: txnComercio={}, estado={}",
                payload.getTransaccionComercioId(), payload.getEstado());

        // Extraer número de solicitud desde TransaccionComercioId
        // Formato: TXN-SOL-YYYY-NNN-timestamp → SOL-YYYY-NNN
        String numeroSolicitud = extraerNumeroSolicitud(payload.getTransaccionComercioId());
        if (numeroSolicitud == null) {
            log.warn("Formato inválido de TransaccionComercioId: {}", payload.getTransaccionComercioId());
            return;
        }

        Solicitud solicitud = solicitudRepository
                .findAll().stream()
                .filter(s -> s.getNumero().equals(numeroSolicitud))
                .findFirst()
                .orElseThrow(() -> new BusinessException("NOT_FOUND",
                        "Solicitud no encontrada para referencia: " + numeroSolicitud));

        Pago pago = pagoRepository.findBySolicitudId(solicitud.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Pago no encontrado."));

        // "REALIZADA" = aprobado en el mock de PlusPagos
        if ("REALIZADA".equalsIgnoreCase(payload.getEstado())) {
            pago.setEstado(EstadoPago.APROBADO);
            pago.setReferencia(payload.getTransaccionPlataformaId());
            pagoRepository.save(pago);
            solicitudService.marcarComoPagada(solicitud);
            log.info("Pago APROBADO para solicitud {} (plataformaId={})",
                    solicitud.getNumero(), payload.getTransaccionPlataformaId());

        } else if ("RECHAZADA".equalsIgnoreCase(payload.getEstado())) {
            pago.setEstado(EstadoPago.RECHAZADO);
            pagoRepository.save(pago);
            solicitudService.marcarComoRechazada(solicitud);
            log.warn("Pago RECHAZADO para solicitud {}", solicitud.getNumero());
        } else {
            log.warn("Estado de pago desconocido recibido: {}", payload.getEstado());
        }
    }

    // ─── Consultar pago ───────────────────────────────────────────────────────

    public PagoDTO getById(Long id) {
        return pagoRepository.findById(id)
                .map(this::toSimpleDTO)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Pago no encontrado."));
    }

    public PagoDTO getPorSolicitud(Long solicitudId) {
        return pagoRepository.findBySolicitudId(solicitudId)
                .map(this::toSimpleDTO)
                .orElseThrow(() -> new BusinessException("NOT_FOUND",
                        "No se encontró pago para la solicitud indicada."));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extrae el número de solicitud desde el TransaccionComercioId.
     * Formato esperado: TXN-SOL-YYYY-NNN-timestamp
     * Ejemplo: TXN-SOL-2026-001-1709559430000 → SOL-2026-001
     */
    private String extraerNumeroSolicitud(String transaccionComercioId) {
        if (transaccionComercioId == null)
            return null;
        // El ID comienza con TXN-SOL-YYYY-NNN, tomamos desde SOL-
        int idx = transaccionComercioId.indexOf("SOL-");
        if (idx < 0)
            return null;
        String sinPrefix = transaccionComercioId.substring(idx); // SOL-2026-001-timestamp
        // El número de solicitud son los primeros 3 segmentos separados por -
        String[] parts = sinPrefix.split("-");
        if (parts.length < 3)
            return null;
        return parts[0] + "-" + parts[1] + "-" + parts[2]; // SOL-2026-001
    }

    private PagoDTO toSimpleDTO(Pago p) {
        return PagoDTO.builder()
                .id(p.getId())
                .solicitudId(p.getSolicitud().getId())
                .monto(p.getMonto())
                .referencia(p.getReferencia())
                .estado(p.getEstado())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
