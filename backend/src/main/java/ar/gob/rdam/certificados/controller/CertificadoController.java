package ar.gob.rdam.certificados.controller;

import ar.gob.rdam.certificados.dto.CertificadoDTO;
import ar.gob.rdam.certificados.service.CertificadoService;
import ar.gob.rdam.domain.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    /**
     * POST /certificados/emitir (GESTOR/ADMIN) — multipart: archivo + solicitudId
     */
    @PostMapping(value = "/emitir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificadoDTO> emitir(
            @RequestParam("solicitudId") Long solicitudId,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal Usuario emisor) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certificadoService.emitir(solicitudId, archivo, emisor));
    }

    /** GET /certificados/:id — Metadatos */
    @GetMapping("/{id}")
    public ResponseEntity<CertificadoDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoService.getById(id));
    }

    /** GET /certificados/:id/descargar — Descarga del PDF */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<FileSystemResource> descargar(@PathVariable Long id) throws IOException {
        CertificadoDTO cert = certificadoService.getById(id);
        File file = new File(cert.getArchivoUrl());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getName() + "\"")
                .body(new FileSystemResource(file));
    }

    /** GET /certificados/solicitud/:solicitudId */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<CertificadoDTO> getPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(certificadoService.getPorSolicitud(solicitudId));
    }

    /** GET /certificados/:id/verificar (público) */
    @GetMapping("/{id}/verificar")
    public ResponseEntity<CertificadoDTO> verificar(@PathVariable Long id) {
        return ResponseEntity.ok(certificadoService.verificar(id));
    }
}
