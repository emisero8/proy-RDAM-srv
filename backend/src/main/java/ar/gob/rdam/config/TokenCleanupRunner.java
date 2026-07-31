package ar.gob.rdam.config;

import ar.gob.rdam.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Invalida todos los tokens de refresco al iniciar la aplicación.
 * Esto fuerza a que todos los usuarios tengan que volver a iniciar sesión,
 * cumpliendo con el requerimiento de seguridad de que los tokens no
 * persistan en validación tras un reinicio del backend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class TokenCleanupRunner implements ApplicationRunner {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🧹 Limpiando tokens de refresco al inicio de la aplicación para forzar re-login...");
        try {
            refreshTokenRepository.deleteAllInBatch();
            log.info("✅ Todos los tokens de refresco han sido eliminados correctamente.");
        } catch (Exception e) {
            log.warn("⚠️ No se pudieron limpiar los tokens de manera masiva, intentando borrado clásico...", e);
            refreshTokenRepository.deleteAll();
            log.info("✅ Todos los tokens de refresco han sido eliminados.");
        }
    }
}
