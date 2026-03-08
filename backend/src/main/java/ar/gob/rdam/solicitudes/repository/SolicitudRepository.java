package ar.gob.rdam.solicitudes.repository;

import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

     @Query("""
               SELECT s FROM Solicitud s
               LEFT JOIN FETCH s.ciudadano c
               LEFT JOIN FETCH s.revisor r
               WHERE s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(c.nombre) LIKE concat('%', :search, '%')
                      OR lower(c.apellido) LIKE concat('%', :search, '%'))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesSinEstado(
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     @Query("""
               SELECT s FROM Solicitud s
               LEFT JOIN FETCH s.ciudadano c
               LEFT JOIN FETCH s.revisor r
               WHERE s.estado = :estado
                 AND s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(c.nombre) LIKE concat('%', :search, '%')
                      OR lower(c.apellido) LIKE concat('%', :search, '%'))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesConEstado(
               @Param("estado") EstadoSolicitud estado,
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     @Query("""
               SELECT s FROM Solicitud s
               LEFT JOIN FETCH s.ciudadano c
               LEFT JOIN FETCH s.revisor r
               WHERE s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(c.nombre) LIKE concat('%', :search, '%')
                      OR lower(c.apellido) LIKE concat('%', :search, '%'))
                 AND (s.estado = ar.gob.rdam.domain.enums.EstadoSolicitud.PENDIENTE_REVISION
                      OR (s.revisor IS NOT NULL AND s.revisor.id = :gestorId))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesGestorSinEstado(
               @Param("gestorId") Long gestorId,
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     @Query("""
               SELECT s FROM Solicitud s
               LEFT JOIN FETCH s.ciudadano c
               LEFT JOIN FETCH s.revisor r
               WHERE s.estado = :estado
                 AND s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(c.nombre) LIKE concat('%', :search, '%')
                      OR lower(c.apellido) LIKE concat('%', :search, '%'))
                 AND (s.estado = ar.gob.rdam.domain.enums.EstadoSolicitud.PENDIENTE_REVISION
                      OR (s.revisor IS NOT NULL AND s.revisor.id = :gestorId))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesGestorConEstado(
               @Param("gestorId") Long gestorId,
               @Param("estado") EstadoSolicitud estado,
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     List<Solicitud> findAllByCiudadanoId(Long ciudadanoId, Pageable pageable);

     long countByEstado(EstadoSolicitud estado);

     @Query("SELECT s FROM Solicitud s WHERE s.estado = 'EMITIDA' AND s.updatedAt < :limite")
     List<Solicitud> findEmitidosExpirados(@Param("limite") LocalDateTime limite);
}
