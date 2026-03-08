package ar.gob.rdam.auth.service;

import ar.gob.rdam.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmailTokenService — Tests Unitarios")
class EmailTokenServiceTest {

    private EmailTokenService service;

    @BeforeEach
    void setUp() {
        service = new EmailTokenService();
        ReflectionTestUtils.setField(service, "codigoTtlMinutos", 15);
    }

    @Test
    @DisplayName("generarToken: genera código de 6 dígitos")
    void generarToken_debeGenerarCodigoDe6Digitos() {
        String codigo = service.generarToken("test@email.com");
        assertNotNull(codigo);
        assertEquals(6, codigo.length());
        assertTrue(codigo.matches("\\d{6}"));
    }

    @Test
    @DisplayName("validarToken: código correcto pasa sin errores")
    void validarToken_codigoCorrecto_debePasarSinErrores() {
        String codigo = service.generarToken("test@email.com");
        assertDoesNotThrow(() -> service.validarToken("test@email.com", codigo));
    }

    @Test
    @DisplayName("validarToken: código incorrecto lanza INVALID_CODE")
    void validarToken_codigoIncorrecto_debeArrojarInvalidCode() {
        service.generarToken("test@email.com");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validarToken("test@email.com", "000000"));
        assertEquals("INVALID_CODE", ex.getCode());
    }

    @Test
    @DisplayName("validarToken: sin código activo lanza INVALID_CODE")
    void validarToken_sinCodigoActivo_debeArrojarInvalidCode() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validarToken("nadie@email.com", "123456"));
        assertEquals("INVALID_CODE", ex.getCode());
    }

    @Test
    @DisplayName("validarToken: código ya consumido (single-use) lanza error")
    void validarToken_codigoYaConsumido_debeArrojarError() {
        String codigo = service.generarToken("test@email.com");
        service.validarToken("test@email.com", codigo);

        // Segundo uso debe fallar
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validarToken("test@email.com", codigo));
        assertEquals("INVALID_CODE", ex.getCode());
    }

    @Test
    @DisplayName("validarToken: email case-insensitive")
    void validarToken_emailCaseInsensitive_debeFuncionar() {
        String codigo = service.generarToken("Test@Email.COM");
        assertDoesNotThrow(() -> service.validarToken("test@email.com", codigo));
    }

    @Test
    @DisplayName("generarToken: genera nuevo código si ya existía uno")
    void generarToken_reemplazaCodigoAnterior() {
        String codigo1 = service.generarToken("test@email.com");
        String codigo2 = service.generarToken("test@email.com");

        // El primer código ya no debería ser válido
        if (!codigo1.equals(codigo2)) {
            assertThrows(BusinessException.class,
                    () -> service.validarToken("test@email.com", codigo1));
        }
        // El segundo sí
        assertDoesNotThrow(() -> service.validarToken("test@email.com", codigo2));
    }

    @Test
    @DisplayName("validarToken: código expirado lanza CODE_EXPIRED")
    void validarToken_codigoExpirado_debeArrojarCodeExpired() throws InterruptedException {
        // Configurar TTL a 0 minutos para forzar expiración
        ReflectionTestUtils.setField(service, "codigoTtlMinutos", 0);
        String codigo = service.generarToken("test@email.com");

        // Esperar un instante para garantizar que el token esté expirado
        Thread.sleep(50);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validarToken("test@email.com", codigo));
        assertEquals("CODE_EXPIRED", ex.getCode());
    }
}
