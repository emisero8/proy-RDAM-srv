package ar.gob.rdam.auth.service;

import ar.gob.rdam.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de tokens efímeros de 6 dígitos para autenticación de ciudadanos.
 * Los tokens se almacenan en memoria (ConcurrentHashMap), tienen un TTL
 * configurable y son de un solo uso.
 */
@Service
@Slf4j
public class EmailTokenService {

    private final SecureRandom random = new SecureRandom();

    /** email → TokenEntry */
    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    @Value("${rdam.auth.codigo-ttl-minutos:15}")
    private int codigoTtlMinutos;

    // ─── Generar ────────────────────────────────────────────────────────────────

    /**
     * Genera un código de 6 dígitos para el email dado.
     * Si ya existía un código previo para ese email, lo reemplaza.
     *
     * @return el código generado
     */
    public String generarToken(String email) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expira = LocalDateTime.now().plusMinutes(codigoTtlMinutos);
        tokens.put(email.toLowerCase(), new TokenEntry(codigo, expira));
        log.info("🔑 Código generado para {}: {} (expira {})", email, codigo, expira);
        return codigo;
    }

    // ─── Validar ────────────────────────────────────────────────────────────────

    /**
     * Valida y consume el token para el email indicado.
     * Lanza BusinessException si el código es inválido o expirado.
     */
    public void validarToken(String email, String codigo) {
        String key = email.toLowerCase();
        TokenEntry entry = tokens.get(key);

        if (entry == null) {
            throw new BusinessException("INVALID_CODE", "No se encontró un código activo para este email.");
        }

        if (entry.expira().isBefore(LocalDateTime.now())) {
            tokens.remove(key);
            throw new BusinessException("CODE_EXPIRED", "El código ha expirado. Solicitá uno nuevo.");
        }

        if (!entry.codigo().equals(codigo)) {
            throw new BusinessException("INVALID_CODE", "El código ingresado es incorrecto.");
        }

        // Consumir (single-use)
        tokens.remove(key);
        log.debug("✅ Código validado y consumido para {}", email);
    }

    // ─── Limpieza periódica ─────────────────────────────────────────────────────

    /** Cada 5 minutos limpia tokens expirados. */
    @Scheduled(fixedRate = 300_000)
    public void limpiarExpirados() {
        LocalDateTime ahora = LocalDateTime.now();
        int antes = tokens.size();
        tokens.entrySet().removeIf(e -> e.getValue().expira().isBefore(ahora));
        int eliminados = antes - tokens.size();
        if (eliminados > 0) {
            log.debug("🧹 Limpieza de tokens: {} eliminados, {} activos", eliminados, tokens.size());
        }
    }

    // ─── Record interno ─────────────────────────────────────────────────────────

    private record TokenEntry(String codigo, LocalDateTime expira) {
    }
}
