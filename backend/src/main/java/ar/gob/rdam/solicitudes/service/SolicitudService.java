package ar.gob.rdam.solicitudes.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.HistorialEstado;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.solicitudes.dto.CrearSolicitudRequest;
import ar.gob.rdam.solicitudes.dto.RechazarSolicitudRequest;
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
import java.time.format.DateTimeFormatter;
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
                .estado(EstadoSolicitud.PENDIENTE_REVISION)
                .arancel(arancelLibreDeuda)
                .build();

        s = solicitudRepository.save(s);
        registrarHistorial(s, null, EstadoSolicitud.PENDIENTE_REVISION, ciudadano, "Solicitud creada");
        return toDTO(s);
    }

    // ─── Listar bandeja interna ───────────────────────────────────────────────

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

    // ─── Listar bandeja gestor (solo propias + pendientes) ────────────────────

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

    // ─── Tomar solicitud (gestor) ─────────────────────────────────────────────

    @Transactional
    public SolicitudDTO tomar(Long id, Usuario revisor) {
        Solicitud s = findOrThrow(id);
        validarTransicion(s.getEstado(), EstadoSolicitud.EN_REVISION);
        s.setRevisor(revisor);
        cambiarEstado(s, EstadoSolicitud.EN_REVISION, revisor, "Solicitud tomada para revisión");
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Aprobar solicitud ────────────────────────────────────────────────────

    @Transactional
    public SolicitudDTO aprobar(Long id, String comentario, Usuario revisor) {
        Solicitud s = findOrThrow(id);
        validarTransicion(s.getEstado(), EstadoSolicitud.APROBADA);
        cambiarEstado(s, EstadoSolicitud.APROBADA, revisor, comentario);
        // Automáticamente pasa a PENDIENTE_PAGO
        cambiarEstado(s, EstadoSolicitud.PENDIENTE_PAGO, revisor, "Pago requerido");
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Rechazar solicitud ───────────────────────────────────────────────────

    @Transactional
    public SolicitudDTO rechazar(Long id, RechazarSolicitudRequest request, Usuario revisor) {
        Solicitud s = findOrThrow(id);
        validarTransicion(s.getEstado(), EstadoSolicitud.RECHAZADA);
        s.setMotivoRechazo(request.getMotivoRechazo());
        cambiarEstado(s, EstadoSolicitud.RECHAZADA, revisor, request.getComentario());
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Cancelar solicitud (ciudadano) ───────────────────────────────────────

    @Transactional
    public SolicitudDTO cancelar(Long id, Usuario ciudadano) {
        Solicitud s = findOrThrow(id);
        // Solo el propio ciudadano puede cancelar
        if (!s.getCiudadano().getId().equals(ciudadano.getId())) {
            throw new BusinessException("FORBIDDEN", "Solo el ciudadano dueño puede cancelar la solicitud.");
        }
        validarTransicion(s.getEstado(), EstadoSolicitud.CANCELADA);
        cambiarEstado(s, EstadoSolicitud.CANCELADA, ciudadano, "Cancelada por el ciudadano");
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Marcar como PAGADA (simulación directa, sin pasarela) ───────────────

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

    // ─── Reasignar gestión (admin pisa al gestor actual) ─────────────────────

    @Transactional
    public SolicitudDTO reasignar(Long id, Usuario admin) {
        Solicitud s = findOrThrow(id);
        if (s.getEstado() != EstadoSolicitud.EN_REVISION) {
            throw new BusinessException("BUSINESS_RULE",
                    "Solo se puede reasignar una solicitud EN_REVISION.");
        }
        String gestorAnterior = s.getRevisor() != null
                ? s.getRevisor().getNombre() + " " + s.getRevisor().getApellido()
                : "sin gestor";
        s.setRevisor(admin);
        registrarHistorial(s, EstadoSolicitud.EN_REVISION, EstadoSolicitud.EN_REVISION,
                admin, "Reasignada por administrador (anterior: " + gestorAnterior + ")");
        return toDTO(solicitudRepository.save(s));
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
        // Cancelar se permite desde cualquier estado no-terminal
        if (destino == EstadoSolicitud.CANCELADA) {
            boolean cancelable = actual != EstadoSolicitud.CANCELADA
                    && actual != EstadoSolicitud.RECHAZADA
                    && actual != EstadoSolicitud.EMITIDA
                    && actual != EstadoSolicitud.EXPIRADA;
            if (!cancelable) {
                throw new BusinessException("INVALID_TRANSITION",
                        "No se puede cancelar una solicitud en estado " + actual);
            }
            return;
        }
        boolean valida = switch (actual) {
            case PENDIENTE_REVISION -> destino == EstadoSolicitud.EN_REVISION;
            case EN_REVISION -> destino == EstadoSolicitud.APROBADA || destino == EstadoSolicitud.RECHAZADA;
            case APROBADA -> destino == EstadoSolicitud.PENDIENTE_PAGO;
            case PENDIENTE_PAGO -> destino == EstadoSolicitud.PAGADA;
            case PAGADA -> destino == EstadoSolicitud.EMITIDA;
            case EMITIDA -> destino == EstadoSolicitud.EXPIRADA;
            default -> false;
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
