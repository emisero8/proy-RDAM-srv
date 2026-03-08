package ar.gob.rdam.solicitudes.repository;

import ar.gob.rdam.domain.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findAllBySolicitudIdOrderByCreatedAtAsc(Long solicitudId);
}
