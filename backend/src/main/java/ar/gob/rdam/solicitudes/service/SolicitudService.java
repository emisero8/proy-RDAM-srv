package ar.gob.rdam.solicitudes.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.HistorialEstado;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.solicitudes.dto.CrearSolicitudRequest;
import ar.gob.rdam.solicitudes.dto.SolicitudDTO;
import ar.gob.rdam.solicitudes.repository.HistorialEstadoRepository;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final HistorialEstadoRepository historialRepository;

    @Value("${rdam.arancel.libre-deuda}")
    private BigDecimal arancelLibreDeuda;

    @Value("${rdam.certificado.validez-dias}")
    private int validezDias;

    // Counter simple para número de solicitud (en prod usar secuencia DB)
    private final AtomicLong counter = new AtomicLong(1);

    // ─── Crear solicitud (ciudadano) ──────────────────────────────────────────
    //
    // Nuevo flujo: la solicitud nace directamente en PENDIENTE_PAGO.
    // El ciudadano paga, y entonces el gestor/admin emite el certificado.
    // No hay etapa de revisión ni aprobación.

    @Transactional
    public SolicitudDTO crear(CrearSolicitudRequest request, Usuario ciudadano) {
        String numero = "SOL-" + LocalDate.now().getYear() + "-"
                + String.format("%03d", counter.getAndIncrement());

        Solicitud s = Solicitud.builder()
                .numero(numero)
                .ciudadano(ciudadano)
                .tipoCert(request.getTipoCert())
                .urgencia(request.getUrgencia())
                .observaciones(request.getObservaciones())
                .estado(EstadoSolicitud.PENDIENTE_PAGO)
                .arancel(arancelLibreDeuda)
                .build();

        s = solicitudRepository.save(s);
        registrarHistorial(s, null, EstadoSolicitud.PENDIENTE_PAGO, ciudadano, "Solicitud creada — pago requerido");
        return toDTO(s);
    }

    // ─── Listar bandeja interna (GESTOR/ADMIN) ────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SolicitudDTO> listarTodas(EstadoSolicitud estado, String tipoCert, String urgencia,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta,
            String search, Pageable pageable) {

        LocalDateTime fd = fechaDesde != null ? fechaDesde : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime fh = fechaHasta != null ? fechaHasta : LocalDateTime.of(2100, 1, 1, 0, 0);
        String tc = tipoCert != null ? tipoCert : "";
        String urg = urgencia != null ? urgencia : "";
        String s = search != null ? search.toLowerCase() : "";

        if (estado == null) {
            return solicitudRepository.buscarSolicitudesSinEstado(tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        } else {
            return solicitudRepository.buscarSolicitudesConEstado(estado, tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        }
    }

    // ─── Listar bandeja gestor ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<SolicitudDTO> listarParaGestor(Long gestorId, EstadoSolicitud estado, String tipoCert,
            String urgencia, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
            String search, Pageable pageable) {

        LocalDateTime fd = fechaDesde != null ? fechaDesde : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime fh = fechaHasta != null ? fechaHasta : LocalDateTime.of(2100, 1, 1, 0, 0);
        String tc = tipoCert != null ? tipoCert : "";
        String urg = urgencia != null ? urgencia : "";
        String s = search != null ? search.toLowerCase() : "";

        if (estado == null) {
            return solicitudRepository.buscarSolicitudesGestorSinEstado(gestorId, tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        } else {
            return solicitudRepository.buscarSolicitudesGestorConEstado(gestorId, estado, tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        }
    }

    // ─── Listar mis solicitudes (ciudadano) ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolicitudDTO> listarMias(Usuario ciudadano, int page, int limit) {
        return solicitudRepository.findAllByCiudadanoId(ciudadano.getId(), PageRequest.of(page, limit))
                .stream().map(this::toDTO).toList();
    }

    // ─── Ver detalle ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SolicitudDTO getById(Long id, Usuario solicitante) {
        Solicitud s = findOrThrow(id);
        // Ciudadano solo puede ver sus propias solicitudes
        if ("ROLE_CIUDADANO".equals(solicitante.getAuthorities().iterator().next().getAuthority())) {
            if (!s.getCiudadano().getId().equals(solicitante.getId())) {
                throw new BusinessException("FORBIDDEN", "No tiene permisos para ver esta solicitud.");
            }
        }
        return toDTO(s);
    }

    // ─── Cancelar solicitud (ciudadano) ───────────────────────────────────────
    // Solo se puede cancelar cuando está PENDIENTE_PAGO (antes de pagar).

    @Transactional
    public SolicitudDTO cancelar(Long id, Usuario ciudadano) {
        Solicitud s = findOrThrow(id);
        if (!s.getCiudadano().getId().equals(ciudadano.getId())) {
            throw new BusinessException("FORBIDDEN", "Solo el ciudadano dueño puede cancelar la solicitud.");
        }
        validarTransicion(s.getEstado(), EstadoSolicitud.CANCELADA);
        cambiarEstado(s, EstadoSolicitud.CANCELADA, ciudadano, "Cancelada por el ciudadano");
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Marcar como PAGADA (llamado desde PagoService/webhook) ──────────────

    @Transactional
    public void marcarComoPagada(Solicitud solicitud) {
        cambiarEstado(solicitud, EstadoSolicitud.PAGADA, null, "Pago registrado");
        solicitudRepository.save(solicitud);
    }

    // ─── Marcar como EMITIDA (llamado desde CertificadoService) ──────────────

    @Transactional
    public void marcarComoEmitida(Solicitud solicitud, Usuario emisor) {
        cambiarEstado(solicitud, EstadoSolicitud.EMITIDA, emisor, "Certificado emitido");
        solicitudRepository.save(solicitud);
    }

    // ─── Job de expiración (cron diario a las 2am) ────────────────────────────

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void expirarCertificados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(validezDias);
        List<Solicitud> expirados = solicitudRepository.findEmitidosExpirados(limite);
        for (Solicitud s : expirados) {
            cambiarEstado(s, EstadoSolicitud.EXPIRADA, null, "Certificado expirado automáticamente");
            solicitudRepository.save(s);
        }
        log.info("Expiración completada: {} certificados expirados", expirados.size());
    }

    // ─── Historial ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolicitudDTO.HistorialEstadoDTO> getHistorial(Long id) {
        return historialRepository.findAllBySolicitudIdOrderByCreatedAtAsc(id)
                .stream().map(h -> SolicitudDTO.HistorialEstadoDTO.builder()
                        .id(h.getId())
                        .estadoAnt(h.getEstadoAnt())
                        .estadoNuevo(h.getEstadoNuevo())
                        .usuarioNombre(h.getUsuario() != null
                                ? h.getUsuario().getNombre() + " " + h.getUsuario().getApellido()
                                : "Sistema")
                        .comentario(h.getComentario())
                        .createdAt(h.getCreatedAt())
                        .build())
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public Solicitud findOrThrow(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Solicitud no encontrada."));
    }

    private void cambiarEstado(Solicitud s, EstadoSolicitud nuevo, Usuario actor, String comentario) {
        EstadoSolicitud anterior = s.getEstado();
        s.setEstado(nuevo);
        registrarHistorial(s, anterior, nuevo, actor, comentario);
    }

    private void registrarHistorial(Solicitud s, EstadoSolicitud ant,
            EstadoSolicitud nuevo, Usuario actor, String comentario) {
        HistorialEstado h = HistorialEstado.builder()
                .solicitud(s)
                .estadoAnt(ant)
                .estadoNuevo(nuevo)
                .usuario(actor)
                .comentario(comentario)
                .build();
        historialRepository.save(h);
    }

    private void validarTransicion(EstadoSolicitud actual, EstadoSolicitud destino) {
        // Cancelar solo se permite desde PENDIENTE_PAGO (antes de pagar)
        if (destino == EstadoSolicitud.CANCELADA) {
            if (actual != EstadoSolicitud.PENDIENTE_PAGO) {
                throw new BusinessException("INVALID_TRANSITION",
                        "Solo se puede cancelar una solicitud que aún no fue pagada.");
            }
            return;
        }
        boolean valida = switch (actual) {
            case PENDIENTE_PAGO -> destino == EstadoSolicitud.PAGADA;
            case PAGADA         -> destino == EstadoSolicitud.EMITIDA;
            case EMITIDA        -> destino == EstadoSolicitud.EXPIRADA;
            default             -> false;
        };
        if (!valida) {
            throw new BusinessException("INVALID_TRANSITION",
                    "Transición no permitida: " + actual + " → " + destino);
        }
    }

    public SolicitudDTO toDTO(Solicitud s) {
        return SolicitudDTO.builder()
                .id(s.getId())
                .numero(s.getNumero())
                .ciudadanoId(s.getCiudadano().getId())
                .ciudadanoNombre(s.getCiudadano().getNombre() + " " + s.getCiudadano().getApellido())
                .ciudadanoEmail(s.getCiudadano().getEmail())
                .tipoCert(s.getTipoCert())
                .urgencia(s.getUrgencia())
                .estado(s.getEstado())
                .arancel(s.getArancel())
                .observaciones(s.getObservaciones())
                .motivoRechazo(s.getMotivoRechazo())
                .revisorId(s.getRevisor() != null ? s.getRevisor().getId() : null)
                .revisorNombre(s.getRevisor() != null
                        ? s.getRevisor().getNombre() + " " + s.getRevisor().getApellido()
                        : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
