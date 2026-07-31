package ar.gob.rdam.auth.service;

import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService — Tests Unitarios")
class JwtServiceTest {

    private JwtService jwtService;
    private Usuario ciudadano;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Clave de al menos 256 bits para HS256
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "a-very-secret-key-that-is-at-least-32-characters-long!!");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(jwtService, "ciudadanoTokenExpiration", 86400L);

        ciudadano = Usuario.builder()
                .id(1L).nombre("María").apellido("García")
                .email("maria@test.com").password(null)
                .tipo(TipoUsuario.CIUDADANO).rol(RolUsuario.CIUDADANO).activo(true)
                .build();

        gestor = Usuario.builder()
                .id(2L).nombre("Laura").apellido("Martínez")
                .email("laura@rdam.gob.ar").password("encoded")
                .tipo(TipoUsuario.INTERNO).rol(RolUsuario.GESTOR).activo(true)
                .circunscripcion("Circunscripción I")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Token Ciudadano
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generateCiudadanoToken: genera token válido")
    void generateCiudadanoToken_debeGenerarTokenValido() {
        String token = jwtService.generateCiudadanoToken("maria@test.com");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("generateCiudadanoToken: claims contienen tipo CIUDADANO")
    void generateCiudadanoToken_debeContenerTipoCiudadano() {
        String token = jwtService.generateCiudadanoToken("maria@test.com");

        assertEquals("maria@test.com", jwtService.extractUsername(token));
        assertEquals("CIUDADANO", jwtService.extractTipo(token));
        assertEquals("CIUDADANO", jwtService.extractRol(token));
        assertEquals("CIUDADANO", jwtService.extractPortal(token));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Token Operador Interno
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generateAccessToken: genera token válido con rol y circunscripción")
    void generateAccessToken_debeGenerarTokenConRolYCircunscripcion() {
        String token = jwtService.generateAccessToken(gestor, "ADMIN");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("laura@rdam.gob.ar", jwtService.extractUsername(token));
        assertEquals("GESTOR", jwtService.extractRol(token));
        assertEquals("ADMIN", jwtService.extractPortal(token));
        assertEquals("INTERNO", jwtService.extractTipo(token));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Validación
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("isTokenValid: token expirado devuelve false")
    void isTokenValid_tokenExpirado_debeDevolverFalse() {
        // Configurar expiración de 0 segundos → token nace expirado
        ReflectionTestUtils.setField(jwtService, "ciudadanoTokenExpiration", 0L);
        String token = jwtService.generateCiudadanoToken("maria@test.com");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid: token manipulado devuelve false")
    void isTokenValid_tokenManipulado_debeDevolverFalse() {
        String token = jwtService.generateCiudadanoToken("maria@test.com");
        // Alterar un carácter en la firma (última parte del JWT)
        char[] chars = token.toCharArray();
        int pos = chars.length - 5;
        chars[pos] = (chars[pos] == 'A') ? 'B' : 'A';
        String tokenManipulado = new String(chars);

        assertFalse(jwtService.isTokenValid(tokenManipulado));
    }
}
