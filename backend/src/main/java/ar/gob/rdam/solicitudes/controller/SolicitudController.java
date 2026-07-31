package ar.gob.rdam.solicitudes.controller;

import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.solicitudes.dto.CrearSolicitudRequest;
import ar.gob.rdam.solicitudes.dto.SolicitudDTO;
import ar.gob.rdam.solicitudes.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    /** GET /solicitudes — Bandeja interna (GESTOR/ADMIN) */
    @GetMapping
    public ResponseEntity<Page<SolicitudDTO>> listarTodas(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) String tipoCert,
            @RequestParam(required = false) String urgencia,
            @RequestParam(required = false) LocalDateTime fechaDesde,
            @RequestParam(required = false) LocalDateTime fechaHasta,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "asc") String order,
            @AuthenticationPrincipal Usuario usuario) {
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(dir, sort));
        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (esAdmin) {
            return ResponseEntity.ok(solicitudService.listarTodas(
                    estado, tipoCert, urgencia, fechaDesde, fechaHasta, search, pageable));
        } else {
            return ResponseEntity.ok(solicitudService.listarParaGestor(
                    usuario.getId(), estado, tipoCert, urgencia, fechaDesde, fechaHasta, search, pageable));
        }
    }

    /** GET /solicitudes/mis — Solicitudes del ciudadano autenticado */
    @GetMapping("/mis")
    public ResponseEntity<List<SolicitudDTO>> listarMias(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        String email = (String) principal;
        return ResponseEntity.ok(solicitudService.listarMias(email, page - 1, limit));
    }

    /** GET /solicitudes/:id — Detalle */
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(solicitudService.getById(id, principal));
    }

    /**
     * POST /solicitudes — Crear nueva solicitud (ciudadano).
     * La solicitud nace directamente en PENDIENTE_PAGO.
     */
    @PostMapping
    public ResponseEntity<SolicitudDTO> crear(
            @Valid @RequestBody CrearSolicitudRequest request,
            @AuthenticationPrincipal Object principal) {
        String email = (String) principal;
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crear(request, email));
    }

    /**
     * PATCH /solicitudes/:id/cancelar — Ciudadano cancela su solicitud.
     * Solo permitido mientras está en PENDIENTE_PAGO (antes de pagar).
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<SolicitudDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Object principal) {
        String email = (String) principal;
        return ResponseEntity.ok(solicitudService.cancelar(id, email));
    }

    /**
     * PATCH /solicitudes/:id/pagar — Simulación de pago directo (sin pasarela).
     * Solo para testing/dev. En producción el pago real viene por webhook de PlusPagos.
     */
    @PatchMapping("/{id}/pagar")
    public ResponseEntity<SolicitudDTO> pagar(
            @PathVariable Long id,
            @AuthenticationPrincipal Object principal) {
        Solicitud s = solicitudService.findOrThrow(id);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE_PAGO) {
            throw new ar.gob.rdam.common.exception.BusinessException("BUSINESS_RULE",
                    "La solicitud no está en estado PENDIENTE_PAGO.");
        }
        solicitudService.marcarComoPagada(s);
        return ResponseEntity.ok(solicitudService.toDTO(s));
    }

    /** GET /solicitudes/:id/historial */
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<SolicitudDTO.HistorialEstadoDTO>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.getHistorial(id));
    }
}
