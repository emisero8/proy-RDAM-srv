package ar.gob.rdam.solicitudes.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.common.service.EmailService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final HistorialEstadoRepository historialRepository;
    private final EmailService emailService;

    @Value("${rdam.arancel.libre-deuda}")
    private BigDecimal arancelLibreDeuda;

    @Value("${rdam.certificado.validez-dias}")
    private int validezDias;


    // ─── Crear solicitud (ciudadano) ──────────────────────────────────────────
    //
    // Nuevo flujo: la solicitud nace directamente en PENDIENTE_PAGO.
    // El ciudadano paga, y entonces el gestor/admin emite el certificado.
    // No hay etapa de revisión ni aprobación.

    @Transactional
    public SolicitudDTO crear(CrearSolicitudRequest request, String email) {
        // Generamos un numero temporal para cumplir con la constraint de NOT NULL y UNIQUE
        String tempNumero = java.util.UUID.randomUUID().toString().substring(0, 20);

        Solicitud s = Solicitud.builder()
                .numero(tempNumero)
                .email(email)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .dni(request.getDni())
                .fechaNacimiento(request.getFechaNacimiento())
                .tipoCert(request.getTipoCert())
                .urgencia(request.getUrgencia())
                .observaciones(request.getObservaciones())
                .estado(EstadoSolicitud.PENDIENTE_PAGO)
                .arancel(arancelLibreDeuda)
                .build();

        s = solicitudRepository.save(s);
        
        // Actualizamos con el numero final usando el ID autogenerado
        s.setNumero("SOL-" + LocalDate.now().getYear() + "-" + String.format("%03d", s.getId()));
        s = solicitudRepository.save(s);

        registrarHistorial(s, null, EstadoSolicitud.PENDIENTE_PAGO, null, "Solicitud creada — pago requerido");
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
            return solicitudRepository.buscarSolicitudesGestorSinEstado(tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        } else {
            return solicitudRepository.buscarSolicitudesGestorConEstado(estado, tc, urg, fd, fh, s, pageable)
                    .map(this::toDTO);
        }
    }

    // ─── Listar mis solicitudes (ciudadano) ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolicitudDTO> listarMias(String email, int page, int limit) {
        return solicitudRepository.findAllByEmail(email, PageRequest.of(page, limit))
                .stream().map(this::toDTO).toList();
    }

    // ─── Ver detalle ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SolicitudDTO getById(Long id, Object solicitante) {
        Solicitud s = findOrThrow(id);
        
        if (solicitante instanceof String email) {
            // Es un ciudadano
            if (!email.equals(s.getEmail())) {
                throw new BusinessException("FORBIDDEN", "No tiene permisos para ver esta solicitud.");
            }
        }
        
        return toDTO(s);
    }

    // ─── Cancelar solicitud (ciudadano) ───────────────────────────────────────
    // Solo se puede cancelar cuando está PENDIENTE_PAGO (antes de pagar).

    @Transactional
    public SolicitudDTO cancelar(Long id, String email) {
        Solicitud s = findOrThrow(id);
        if (!email.equals(s.getEmail())) {
            throw new BusinessException("FORBIDDEN", "Solo el ciudadano dueño puede cancelar la solicitud.");
        }
        validarTransicion(s.getEstado(), EstadoSolicitud.CANCELADA);
        cambiarEstado(s, EstadoSolicitud.CANCELADA, null, "Cancelada por el ciudadano: " + email);
        return toDTO(solicitudRepository.save(s));
    }

    // ─── Marcar como PAGADA (llamado desde PagoService/webhook) ──────────────

    @Transactional
    public void marcarComoPagada(Solicitud solicitud) {
        cambiarEstado(solicitud, EstadoSolicitud.PAGADA, null, "Pago registrado");
        solicitudRepository.save(solicitud);
    }

    // ─── Marcar como RECHAZADA (llamado desde PagoService/webhook) ───────────

    @Transactional
    public void marcarComoRechazada(Solicitud solicitud) {
        cambiarEstado(solicitud, EstadoSolicitud.RECHAZADA, null, "Pago rechazado por la pasarela");
        solicitudRepository.save(solicitud);
    }

    // ─── Marcar como EMITIDA (llamado desde CertificadoService) ──────────────

    @Transactional
    public void marcarComoEmitida(Solicitud solicitud, Usuario emisor) {
        solicitud.setRevisor(emisor);
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

        // Notificar al ciudadano por email del cambio de estado
        try {
            emailService.enviarCambioEstado(
                    s.getEmail(),
                    s.getNombre() + " " + s.getApellido(),
                    s.getNumero(),
                    anterior,
                    nuevo,
                    comentario
            );
        } catch (Exception e) {
            log.error("Error al enviar notificación de cambio de estado: {}", e.getMessage());
        }
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
            case PENDIENTE_PAGO -> destino == EstadoSolicitud.PAGADA || destino == EstadoSolicitud.RECHAZADA;
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
                .ciudadanoNombre(s.getNombre() + " " + s.getApellido())
                .ciudadanoEmail(s.getEmail())
                .ciudadanoDni(s.getDni())
                .ciudadanoFechaNacimiento(s.getFechaNacimiento())
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
