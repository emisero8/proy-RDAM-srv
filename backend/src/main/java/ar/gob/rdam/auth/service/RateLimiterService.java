package ar.gob.rdam.auth.service;

import ar.gob.rdam.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting en memoria para solicitud de códigos de verificación.
 * Limita a N intentos por hora por email.
 */
@Service
@Slf4j
public class RateLimiterService {

    /** email → RateLimitEntry */
    private final Map<String, RateLimitEntry> limites = new ConcurrentHashMap<>();

    @Value("${rdam.auth.codigo-max-intentos:5}")
    private int maxIntentos;

    /**
     * Verifica que el email no haya excedido el límite de solicitudes por hora.
     * Si lo excedió, lanza BusinessException.
     */
    public void verificarLimite(String email) {
        String key = email.toLowerCase();
        LocalDateTime ahora = LocalDateTime.now();

        RateLimitEntry entry = limites.compute(key, (k, v) -> {
            if (v == null || v.ventanaInicio().plusHours(1).isBefore(ahora)) {
                // Primera solicitud o ventana expirada → nueva ventana
                return new RateLimitEntry(ahora, new AtomicInteger(1));
            }
            v.intentos().incrementAndGet();
            return v;
        });

        if (entry.intentos().get() > maxIntentos) {
            log.warn("⚠️ Rate limit excedido para {}: {} intentos", email, entry.intentos().get());
            throw new BusinessException("RATE_LIMITED",
                    "Has excedido el límite de solicitudes. Intentá nuevamente en una hora.");
        }
    }

    /** Cada 10 minutos limpia entradas con ventana expirada. */
    @Scheduled(fixedRate = 600_000)
    public void limpiarExpirados() {
        LocalDateTime ahora = LocalDateTime.now();
        limites.entrySet().removeIf(e -> e.getValue().ventanaInicio().plusHours(1).isBefore(ahora));
    }

    private record RateLimitEntry(LocalDateTime ventanaInicio, AtomicInteger intentos) {
    }
}
