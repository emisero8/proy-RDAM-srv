package ar.gob.rdam.auth.service;

import ar.gob.rdam.auth.dto.*;
import ar.gob.rdam.common.service.EmailService;
import ar.gob.rdam.auth.repository.RefreshTokenRepository;
import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.RefreshToken;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenService emailTokenService;
    private final RateLimiterService rateLimiterService;
    private final EmailService emailService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // segundos

    // ═══════════════════════════════════════════════════════════════════════════
    // FLUJO CIUDADANO — Token por Email (sin contraseña)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Paso 1: El ciudadano solicita un código de verificación.
     * Se aplica rate limiting (máx 5/hora) y se genera un código efímero de 6
     * dígitos.
     * En perfil dev el código se loguea en consola; en producción se enviaría por
     * email.
     */
    public Map<String, String> solicitarCodigo(String email) {
        rateLimiterService.verificarLimite(email);
        String codigo = emailTokenService.generarToken(email);

        // Enviar email real con el código de verificación
        emailService.enviarCodigoLogin(email, codigo);

        // Mantener log en consola para practicidad en desarrollo
        log.info("📧 [DEV] Código de verificación para {}: {}", email, codigo);

        return Map.of("message", "Código enviado al email " + email + ". Válido por 15 minutos.");
    }

    /**
     * Paso 2: El ciudadano valida el código recibido.
     * Si es válido, se busca o crea al usuario ciudadano y se emite un JWT de 24hs.
     */
    @Transactional
    public AuthResponse validarCodigo(String email, String codigo) {
        emailTokenService.validarToken(email, codigo);

        // Validar que no sea un usuario interno intentando usar el portal ciudadano
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            if (usuario.getTipo() != TipoUsuario.CIUDADANO) {
                throw new BusinessException("FORBIDDEN",
                        "Este email pertenece a un usuario interno. Usá el portal de administración.");
            }
        });

        return buildCiudadanoAuthResponse(email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FLUJO INTERNO — Credenciales + JWT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse loginAdmin(LoginRequest request) {
        Usuario usuario = resolveUsuario(request.getIdentificador());
        validarCredenciales(usuario, request.getPassword());

        if (usuario.getTipo() != TipoUsuario.INTERNO) {
            throw new BusinessException("FORBIDDEN",
                    "Acceso denegado. Este portal es solo para usuarios internos.");
        }
        if (!usuario.getActivo()) {
            throw new BusinessException("USER_INACTIVE", "El usuario está desactivado.");
        }

        return buildAdminAuthResponse(usuario);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Refresh token inválido."));

        if (rt.getRevocado() || rt.isExpired()) {
            throw new BusinessException("UNAUTHORIZED", "Refresh token expirado o revocado.");
        }

        // Rotación del refresh token
        rt.setRevocado(true);
        refreshTokenRepository.save(rt);

        String portal = rt.getLoginPortal();
        if ("CIUDADANO".equals(portal)) {
            return buildCiudadanoAuthResponse(rt.getEmail());
        } else {
            return buildAdminAuthResponse(rt.getUsuario());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void logout(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NO SE REQUIERE COMPLETAR PERFIL PARA CIUDADANOS (datos en Solicitud)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void logoutAll(Usuario usuario) {
        refreshTokenRepository.revocarTodosByUsuario(usuario);
    }

    @Transactional
    public void logoutAllByEmail(String email) {
        refreshTokenRepository.revocarTodosByEmail(email);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════════

    private Usuario resolveUsuario(String identificador) {
        return usuarioRepository.findByEmail(identificador)
                .or(() -> usuarioRepository.findByDniCuil(identificador))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas."));
    }

    private void validarCredenciales(Usuario usuario, String rawPassword) {
        if (usuario.getPassword() == null ||
                !passwordEncoder.matches(rawPassword, usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }
    }

    /**
     * Construye respuesta para ciudadano: JWT de 24hs sin info de roles/permisos
     * avanzados.
     */
    private AuthResponse buildCiudadanoAuthResponse(String email) {
        String accessToken = jwtService.generateCiudadanoToken(email);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .token(refreshTokenValue)
                .email(email)
                .loginPortal("CIUDADANO")
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration))
                .build();
        refreshTokenRepository.save(rt);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getCiudadanoTokenExpiration())
                .portal("CIUDADANO")
                .usuario(AuthResponse.UsuarioInfo.builder()
                        .email(email)
                        .rol("CIUDADANO")
                        .perfilCompleto(true)
                        .build())
                .build();
    }

    /**
     * Construye respuesta para operador interno: JWT con rol, permisos,
     * circunscripción.
     */
    private AuthResponse buildAdminAuthResponse(Usuario usuario) {
        String accessToken = jwtService.generateAccessToken(usuario, "ADMIN");
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .token(refreshTokenValue)
                .usuario(usuario)
                .loginPortal("ADMIN")
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration))
                .build();
        refreshTokenRepository.save(rt);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtService.getAccessTokenExpiration())
                .portal("ADMIN")
                .usuario(AuthResponse.UsuarioInfo.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .apellido(usuario.getApellido())
                        .email(usuario.getEmail())
                        .rol(usuario.getRol().name())
                        .build())
                .build();
    }
}
