package ar.gob.rdam.reportes.service;

import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.pagos.repository.PagoRepository;
import ar.gob.rdam.reportes.dto.ResumenDTO;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final SolicitudRepository solicitudRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public ResumenDTO obtenerResumen() {
        String periodo = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Conteo por estado
        long pendRev = solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE_REVISION);
        long enRev = solicitudRepository.countByEstado(EstadoSolicitud.EN_REVISION);
        long aprobadas = solicitudRepository.countByEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudRepository.countByEstado(EstadoSolicitud.RECHAZADA);
        long pendPago = solicitudRepository.countByEstado(EstadoSolicitud.PENDIENTE_PAGO);
        long pagadas = solicitudRepository.countByEstado(EstadoSolicitud.PAGADA);
        long emitidas = solicitudRepository.countByEstado(EstadoSolicitud.EMITIDA);
        long expiradas = solicitudRepository.countByEstado(EstadoSolicitud.EXPIRADA);
        long total = pendRev + enRev + aprobadas + rechazadas + pendPago + pagadas + emitidas + expiradas;

        // Ingresos del mes: suma de pagos aprobados del mes actual
        BigDecimal ingresos = pagoRepository.findAll().stream()
                .filter(p -> p.getEstado().name().equals("APROBADO")
                        && p.getCreatedAt().getMonth() == LocalDate.now().getMonth()
                        && p.getCreatedAt().getYear() == LocalDate.now().getYear())
                .map(p -> p.getMonto())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResumenDTO.builder()
                .periodo(periodo)
                .solicitudes(ResumenDTO.SolicitudesResumen.builder()
                        .total(total)
                        .pendienteRevision(pendRev)
                        .enRevision(enRev)
                        .aprobadas(aprobadas)
                        .rechazadas(rechazadas)
                        .pendientePago(pendPago)
                        .pagadas(pagadas)
                        .emitidas(emitidas)
                        .expiradas(expiradas)
                        .build())
                .tiempoPromedioResolucionHs(0.0) // requiere cálculo con historial
                .ingresosMes(ingresos)
                .build();
    }
}
