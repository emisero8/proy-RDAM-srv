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
               LEFT JOIN FETCH s.revisor r
               WHERE s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(s.numero) LIKE concat('%', :search, '%')
                      OR lower(s.nombre) LIKE concat('%', :search, '%')
                      OR lower(s.apellido) LIKE concat('%', :search, '%')
                      OR lower(s.dni) LIKE concat('%', :search, '%')
                      OR lower(s.email) LIKE concat('%', :search, '%'))
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
               LEFT JOIN FETCH s.revisor r
               WHERE s.estado = :estado
                 AND s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(s.numero) LIKE concat('%', :search, '%')
                      OR lower(s.nombre) LIKE concat('%', :search, '%')
                      OR lower(s.apellido) LIKE concat('%', :search, '%')
                      OR lower(s.dni) LIKE concat('%', :search, '%')
                      OR lower(s.email) LIKE concat('%', :search, '%'))
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
               LEFT JOIN FETCH s.revisor r
               WHERE s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(s.numero) LIKE concat('%', :search, '%')
                      OR lower(s.nombre) LIKE concat('%', :search, '%')
                      OR lower(s.apellido) LIKE concat('%', :search, '%')
                      OR lower(s.dni) LIKE concat('%', :search, '%')
                      OR lower(s.email) LIKE concat('%', :search, '%'))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesGestorSinEstado(
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     @Query("""
               SELECT s FROM Solicitud s
               LEFT JOIN FETCH s.revisor r
               WHERE s.estado = :estado
                 AND s.createdAt >= :fechaDesde
                 AND s.createdAt <= :fechaHasta
                 AND (:tipoCert = '' OR s.tipoCert = :tipoCert)
                 AND (:urgencia = '' OR s.urgencia = :urgencia)
                 AND (:search = ''
                      OR lower(s.numero) LIKE concat('%', :search, '%')
                      OR lower(s.nombre) LIKE concat('%', :search, '%')
                      OR lower(s.apellido) LIKE concat('%', :search, '%')
                      OR lower(s.dni) LIKE concat('%', :search, '%')
                      OR lower(s.email) LIKE concat('%', :search, '%'))
               ORDER BY s.createdAt DESC
               """)
     Page<Solicitud> buscarSolicitudesGestorConEstado(
               @Param("estado") EstadoSolicitud estado,
               @Param("tipoCert") String tipoCert,
               @Param("urgencia") String urgencia,
               @Param("fechaDesde") LocalDateTime fechaDesde,
               @Param("fechaHasta") LocalDateTime fechaHasta,
               @Param("search") String search,
               Pageable pageable);

     List<Solicitud> findAllByEmail(String email, Pageable pageable);

     long countByEstado(EstadoSolicitud estado);

     @Query("SELECT s FROM Solicitud s WHERE s.estado = 'EMITIDA' AND s.updatedAt < :limite")
     List<Solicitud> findEmitidosExpirados(@Param("limite") LocalDateTime limite);
}
