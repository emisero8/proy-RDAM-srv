package ar.gob.rdam.auth.service;

import ar.gob.rdam.auth.dto.*;
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

        // TODO: En producción enviar por email real via JavaMailSender
        // mailService.enviarCodigoVerificacion(email, codigo);
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

        // Buscar ciudadano existente o crear uno nuevo (sin contraseña)
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creando usuario ciudadano automático para: {}", email);
                    Usuario nuevo = Usuario.builder()
                            .nombre("Ciudadano")
                            .apellido("")
                            .email(email)
                            .password(null) // sin contraseña
                            .tipo(TipoUsuario.CIUDADANO)
                            .rol(RolUsuario.CIUDADANO)
                            .activo(true)
                            .perfilCompleto(false)
                            .build();
                    return usuarioRepository.save(nuevo);
                });

        if (usuario.getTipo() != TipoUsuario.CIUDADANO) {
            throw new BusinessException("FORBIDDEN",
                    "Este email pertenece a un usuario interno. Usá el portal de administración.");
        }
        if (!usuario.getActivo()) {
            throw new BusinessException("USER_INACTIVE", "El usuario está desactivado.");
        }

        return buildCiudadanoAuthResponse(usuario);
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
            return buildCiudadanoAuthResponse(rt.getUsuario());
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
    // COMPLETAR PERFIL — Onboarding primer ingreso
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * El ciudadano completa sus datos básicos en el primer ingreso.
     * Si es menor de 18 años, se elimina la cuenta y se lanza una excepción.
     */
    @Transactional
    public AuthResponse completarPerfil(Usuario usuario, CompletarPerfilRequest request) {
        // Validar mayoría de edad
        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Intento de registro de menor de edad para: {}", usuario.getEmail());
            // Revocar todos los tokens antes de borrar
            refreshTokenRepository.revocarTodosByUsuario(usuario);
            // Eliminar el usuario del sistema
            usuarioRepository.deleteById(usuario.getId());
            throw new BusinessException("MENOR_DE_EDAD",
                    "Debés ser mayor de 18 años para utilizar este servicio. La cuenta ha sido eliminada.");
        }

        // Completar perfil del ciudadano
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setDniCuil(request.getDniCuil().trim());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setPerfilCompleto(true);
        usuarioRepository.save(usuario);

        log.info("Perfil completado para ciudadano: {}", usuario.getEmail());
        return buildCiudadanoAuthResponse(usuario);
    }

    @Transactional
    public void logoutAll(Usuario usuario) {
        refreshTokenRepository.revocarTodosByUsuario(usuario);
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
    private AuthResponse buildCiudadanoAuthResponse(Usuario usuario) {
        String accessToken = jwtService.generateCiudadanoToken(usuario);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .token(refreshTokenValue)
                .usuario(usuario)
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
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .apellido(usuario.getApellido())
                        .email(usuario.getEmail())
                        .rol(usuario.getRol().name())
                        .perfilCompleto(Boolean.TRUE.equals(usuario.getPerfilCompleto()))
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
