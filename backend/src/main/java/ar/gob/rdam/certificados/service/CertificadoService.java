package ar.gob.rdam.certificados.service;

import ar.gob.rdam.certificados.dto.CertificadoDTO;
import ar.gob.rdam.certificados.repository.CertificadoRepository;
import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.Certificado;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import ar.gob.rdam.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadoService {

        private final CertificadoRepository certificadoRepository;
        private final SolicitudRepository solicitudRepository;
        private final SolicitudService solicitudService;

        @Value("${rdam.certificado.storage-path}")
        private String storagePath;

        @Value("${rdam.certificado.validez-dias}")
        private int validezDias;

        // ─── Emitir certificado (con archivo PDF subido) ─────────────────────────

        @Transactional
        public CertificadoDTO emitir(Long solicitudId, MultipartFile archivo, Usuario emisor) throws IOException {
                Solicitud solicitud = solicitudRepository.findById(solicitudId)
                                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Solicitud no encontrada."));

                if (solicitud.getEstado() != EstadoSolicitud.PAGADA) {
                        throw new BusinessException("BUSINESS_RULE",
                                        "La solicitud debe estar en estado PAGADA para emitir el certificado.");
                }

                // Verificar que no tenga ya un certificado emitido
                certificadoRepository.findBySolicitudId(solicitud.getId()).ifPresent(c -> {
                        throw new BusinessException("CONFLICT", "Ya existe un certificado para esta solicitud.");
                });

                // Validar que se subió un archivo PDF
                if (archivo == null || archivo.isEmpty()) {
                        throw new BusinessException("VALIDATION", "Debe adjuntar el archivo PDF del certificado.");
                }

                String contentType = archivo.getContentType();
                if (contentType == null || !contentType.equals("application/pdf")) {
                        throw new BusinessException("VALIDATION", "Solo se permiten archivos PDF.");
                }

                // Guardar archivo en disco
                String fileName = "CERT-" + solicitud.getNumero() + ".pdf";
                Path dir = Path.of(storagePath);
                Files.createDirectories(dir);
                Path filePath = dir.resolve(fileName);
                archivo.transferTo(filePath);

                // Firma digital (simulada con UUID)
                String firma = UUID.randomUUID().toString();
                LocalDate vencimiento = LocalDate.now().plusDays(validezDias);

                Certificado cert = Certificado.builder()
                                .solicitud(solicitud)
                                .archivoUrl(filePath.toString())
                                .emisor(emisor)
                                .firmaDigital(firma)
                                .fechaVencimiento(vencimiento)
                                .build();

                cert = certificadoRepository.save(cert);
                solicitudService.marcarComoEmitida(solicitud, emisor);

                log.info("Certificado emitido para solicitud {}: {}", solicitud.getNumero(), fileName);
                return toDTO(cert);
        }

        // ─── Ver metadatos ────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public CertificadoDTO getById(Long id) {
                return certificadoRepository.findById(id)
                                .map(this::toDTO)
                                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Certificado no encontrado."));
        }

        @Transactional(readOnly = true)
        public CertificadoDTO getPorSolicitud(Long solicitudId) {
                return certificadoRepository.findBySolicitudId(solicitudId)
                                .map(this::toDTO)
                                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Certificado no encontrado."));
        }

        // ─── Verificar autenticidad (público) ─────────────────────────────────────

        @Transactional(readOnly = true)
        public CertificadoDTO verificar(Long id) {
                return certificadoRepository.findById(id)
                                .map(cert -> {
                                        boolean valido = cert.getFechaVencimiento() != null
                                                        && !cert.getFechaVencimiento().isBefore(LocalDate.now());
                                        return CertificadoDTO.builder()
                                                        .id(cert.getId())
                                                        .solicitudId(cert.getSolicitud().getId())
                                                        .numeroCertificado(cert.getSolicitud().getNumero())
                                                        .emisorNombre("Municipalidad de RDAM")
                                                        .fechaVencimiento(cert.getFechaVencimiento())
                                                        .createdAt(cert.getCreatedAt())
                                                        .valido(valido)
                                                        .razon(valido ? null : "EXPIRADO")
                                                        .build();
                                })
                                .orElse(CertificadoDTO.builder().valido(false).razon("NOT_FOUND").build());
        }

        // ─── Helper ───────────────────────────────────────────────────────────────

        private CertificadoDTO toDTO(Certificado c) {
                return CertificadoDTO.builder()
                                .id(c.getId())
                                .solicitudId(c.getSolicitud().getId())
                                .numeroCertificado(c.getSolicitud().getNumero())
                                .archivoUrl(c.getArchivoUrl())
                                .emisorNombre(c.getEmisor() != null
                                                ? c.getEmisor().getNombre() + " " + c.getEmisor().getApellido()
                                                : "Sistema")
                                .firmaDigital(c.getFirmaDigital())
                                .fechaVencimiento(c.getFechaVencimiento())
                                .createdAt(c.getCreatedAt())
                                .build();
        }
}
