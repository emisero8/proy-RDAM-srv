package ar.gob.rdam.auth.controller;

import ar.gob.rdam.auth.dto.*;
import ar.gob.rdam.auth.service.AuthService;
import ar.gob.rdam.domain.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ═══════════════════════════════════════════════════════════════════════════
    // FLUJO CIUDADANO — Token por Email
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /auth/solicitar-codigo
     * Paso 1: Ciudadano ingresa su email → recibe código de 6 dígitos.
     */
    @PostMapping("/solicitar-codigo")
    public ResponseEntity<Map<String, String>> solicitarCodigo(
            @Valid @RequestBody SolicitarCodigoRequest request) {
        return ResponseEntity.ok(authService.solicitarCodigo(request.getEmail()));
    }

    /**
     * POST /auth/validar-codigo
     * Paso 2: Ciudadano ingresa el código recibido → obtiene JWT de 24hs.
     */
    @PostMapping("/validar-codigo")
    public ResponseEntity<AuthResponse> validarCodigo(
            @Valid @RequestBody ValidarCodigoRequest request) {
        return ResponseEntity.ok(
                authService.validarCodigo(request.getEmail(), request.getCodigo()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // (completar-perfil eliminado, los datos se manejan al crear Solicitud)
    // ═══════════════════════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════════════════════
    // FLUJO INTERNO — Credenciales + JWT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /auth/admin/login
     * Portal Interno — solo tipo=INTERNO (GESTOR o ADMIN)
     */
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH / LOGOUT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /auth/refresh
     * Renovar access token con refresh token (rotación incluida)
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * POST /auth/logout
     * Revocar el refresh token activo
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente."));
    }

    /**
     * POST /auth/logout-all
     * Revocar todos los refresh tokens del usuario autenticado
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@AuthenticationPrincipal Object principal) {
        if (principal instanceof Usuario) {
            authService.logoutAll((Usuario) principal);
        } else if (principal instanceof String) {
            authService.logoutAllByEmail((String) principal);
        }
        return ResponseEntity.ok(Map.of("message", "Todas las sesiones han sido cerradas."));
    }
}
