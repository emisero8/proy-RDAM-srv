package ar.gob.rdam.auth.service;

import ar.gob.rdam.auth.dto.LoginRequest;
import ar.gob.rdam.auth.repository.RefreshTokenRepository;
import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.common.service.EmailService;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests Unitarios")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailTokenService emailTokenService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Usuario ciudadano;
    private Usuario gestor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800L);

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
    // FLUJO CIUDADANO — Token por Email
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("solicitarCodigo: genera y retorna mensaje")
    void solicitarCodigo_debeGenerarCodigoYRetornarMensaje() {
        when(emailTokenService.generarToken("maria@test.com")).thenReturn("123456");

        var result = authService.solicitarCodigo("maria@test.com");

        assertNotNull(result);
        assertTrue(result.get("message").contains("maria@test.com"));
        verify(rateLimiterService).verificarLimite("maria@test.com");
        verify(emailTokenService).generarToken("maria@test.com");
        verify(emailService).enviarCodigoLogin("maria@test.com", "123456");
    }

    @Test
    @DisplayName("solicitarCodigo: rate limit excedido lanza excepción")
    void solicitarCodigo_rateLimitExcedido_debeArrojarBusinessException() {
        doThrow(new BusinessException("RATE_LIMITED", "Límite excedido"))
                .when(rateLimiterService).verificarLimite("maria@test.com");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.solicitarCodigo("maria@test.com"));
        assertEquals("RATE_LIMITED", ex.getCode());
    }

    @Test
    @DisplayName("validarCodigo: código válido devuelve token")
    void validarCodigo_codigoValido_debeRetornarToken() {
        doNothing().when(emailTokenService).validarToken("maria@test.com", "123456");
        when(jwtService.generateCiudadanoToken(anyString())).thenReturn("jwt-token");
        when(jwtService.getCiudadanoTokenExpiration()).thenReturn(86400L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = authService.validarCodigo("maria@test.com", "123456");

        assertNotNull(response);
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("CIUDADANO", response.getPortal());
    }



    @Test
    @DisplayName("validarCodigo: código inválido lanza excepción")
    void validarCodigo_codigoInvalido_debeArrojarBusinessException() {
        doThrow(new BusinessException("INVALID_CODE", "Código incorrecto"))
                .when(emailTokenService).validarToken("maria@test.com", "000000");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.validarCodigo("maria@test.com", "000000"));
        assertEquals("INVALID_CODE", ex.getCode());
    }

    @Test
    @DisplayName("validarCodigo: email de usuario interno rechazado")
    void validarCodigo_usuarioInterno_debeArrojarForbidden() {
        doNothing().when(emailTokenService).validarToken("laura@rdam.gob.ar", "123456");
        when(usuarioRepository.findByEmail("laura@rdam.gob.ar")).thenReturn(Optional.of(gestor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.validarCodigo("laura@rdam.gob.ar", "123456"));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLUJO INTERNO — Login Admin
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("loginAdmin: credenciales válidas devuelve token con portal ADMIN")
    void loginAdmin_credencialesValidas_debeRetornarToken() {
        when(usuarioRepository.findByEmail("laura@rdam.gob.ar")).thenReturn(Optional.of(gestor));
        when(passwordEncoder.matches("Pass1!", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), anyString())).thenReturn("admin-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setIdentificador("laura@rdam.gob.ar");
        req.setPassword("Pass1!");

        var response = authService.loginAdmin(req);

        assertNotNull(response);
        assertEquals("admin-token", response.getAccessToken());
        assertEquals("ADMIN", response.getPortal());
    }

    @Test
    @DisplayName("loginAdmin: ciudadano rechazado en portal admin")
    void loginAdmin_ciudadano_debeArrojarForbidden() {
        // Ciudadano no tiene password, así que esto debería fallar en credenciales
        ciudadano.setPassword("encoded");
        when(usuarioRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(ciudadano));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setIdentificador("maria@test.com");
        req.setPassword("Pass1!");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginAdmin(req));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    @DisplayName("loginAdmin: contraseña incorrecta lanza BadCredentialsException")
    void loginAdmin_passwordIncorrecta_debeArrojarBadCredentials() {
        when(usuarioRepository.findByEmail("laura@rdam.gob.ar")).thenReturn(Optional.of(gestor));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setIdentificador("laura@rdam.gob.ar");
        req.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authService.loginAdmin(req));
    }
}
