package ar.gob.rdam.pagos.repository;

import ar.gob.rdam.domain.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findBySolicitudId(Long solicitudId);

    Optional<Pago> findByReferencia(String referencia);
}
