package ar.gob.rdam.certificados.repository;

import ar.gob.rdam.domain.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    Optional<Certificado> findBySolicitudId(Long solicitudId);

    Optional<Certificado> findByFirmaDigital(String firmaDigital);
}
