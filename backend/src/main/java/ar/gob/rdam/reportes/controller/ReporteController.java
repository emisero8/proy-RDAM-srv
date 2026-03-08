package ar.gob.rdam.reportes.controller;

import ar.gob.rdam.reportes.dto.ResumenDTO;
import ar.gob.rdam.reportes.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /** GET /reportes/resumen (ADMIN) */
    @GetMapping("/resumen")
    public ResponseEntity<ResumenDTO> resumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }

    // TODO: /reportes/solicitudes, /reportes/tiempos, /reportes/gestores,
    // /reportes/exportar
}
