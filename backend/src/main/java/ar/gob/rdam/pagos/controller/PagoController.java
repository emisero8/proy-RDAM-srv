package ar.gob.rdam.pagos.controller;

import ar.gob.rdam.pagos.dto.IniciarPagoRequest;
import ar.gob.rdam.pagos.dto.PagoDTO;
import ar.gob.rdam.pagos.dto.WebhookPlusPagosRequest;
import ar.gob.rdam.pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    /** POST /pagos/iniciar — ciudadano inicia el pago */
    @PostMapping("/iniciar")
    public ResponseEntity<PagoDTO> iniciar(@Valid @RequestBody IniciarPagoRequest request) {
        return ResponseEntity.ok(pagoService.iniciarPago(request));
    }

    /** GET /pagos/:id */
    @GetMapping("/{id}")
    public ResponseEntity<PagoDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(pagoService.getById(id));
    }

    /** GET /pagos/solicitud/:solicitudId */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<PagoDTO> getPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(pagoService.getPorSolicitud(solicitudId));
    }

    /**
     * POST /pagos/webhook — PlusPagos notifica el resultado del pago
     * Nota: validación HMAC se agrega como interceptor o filtro en producción
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> webhook(@RequestBody WebhookPlusPagosRequest payload) {
        pagoService.procesarWebhook(payload);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
