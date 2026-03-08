package ar.gob.rdam.auth.service;

import ar.gob.rdam.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiterService — Tests Unitarios")
class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService();
        ReflectionTestUtils.setField(service, "maxIntentos", 5);
    }

    @Test
    @DisplayName("verificarLimite: primer intento pasa sin error")
    void verificarLimite_primerIntento_debePasarSinError() {
        assertDoesNotThrow(() -> service.verificarLimite("test@email.com"));
    }

    @Test
    @DisplayName("verificarLimite: 5 intentos permitidos dentro de la ventana")
    void verificarLimite_cincoIntentos_debePermitirTodos() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> service.verificarLimite("test@email.com"));
        }
    }

    @Test
    @DisplayName("verificarLimite: 6to intento lanza RATE_LIMITED")
    void verificarLimite_sextoIntento_debeArrojarRateLimited() {
        for (int i = 0; i < 5; i++) {
            service.verificarLimite("test@email.com");
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verificarLimite("test@email.com"));
        assertEquals("RATE_LIMITED", ex.getCode());
    }

    @Test
    @DisplayName("verificarLimite: emails distintos tienen contadores independientes")
    void verificarLimite_emailsDistintos_contadoresIndependientes() {
        for (int i = 0; i < 5; i++) {
            service.verificarLimite("user1@email.com");
        }

        // user2 debería poder solicitar sin problemas
        assertDoesNotThrow(() -> service.verificarLimite("user2@email.com"));
    }

    @Test
    @DisplayName("verificarLimite: email case-insensitive")
    void verificarLimite_emailCaseInsensitive_mismoContador() {
        for (int i = 0; i < 3; i++) {
            service.verificarLimite("Test@Email.COM");
        }
        // Quedan 2 intentos para el mismo email en minúsculas
        assertDoesNotThrow(() -> service.verificarLimite("test@email.com"));
        assertDoesNotThrow(() -> service.verificarLimite("TEST@EMAIL.COM"));

        // 6to intento con otra variante de case → debe fallar
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verificarLimite("test@email.com"));
        assertEquals("RATE_LIMITED", ex.getCode());
    }
}
