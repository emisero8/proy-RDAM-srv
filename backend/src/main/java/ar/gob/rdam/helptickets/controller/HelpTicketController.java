package ar.gob.rdam.helptickets.controller;

import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.helptickets.dto.CrearTicketRequest;
import ar.gob.rdam.helptickets.dto.HelpTicketDTO;
import ar.gob.rdam.helptickets.service.HelpTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/help-tickets")
@RequiredArgsConstructor
public class HelpTicketController {

    private final HelpTicketService helpTicketService;

    /** POST /help-tickets — Cualquier usuario autenticado crea un ticket */
    @PostMapping
    public ResponseEntity<HelpTicketDTO> crear(
            @Valid @RequestBody CrearTicketRequest request,
            @AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(helpTicketService.crear(email, request));
    }

    /** GET /help-tickets — Admin lista todos los tickets */
    @GetMapping
    public ResponseEntity<List<HelpTicketDTO>> listarTodos() {
        return ResponseEntity.ok(helpTicketService.listarTodos());
    }

    /** DELETE /help-tickets/:id — Admin elimina (cierra) un ticket */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        helpTicketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private String extractEmail(Object principal) {
        if (principal instanceof Usuario usuario) {
            return usuario.getEmail();
        }
        return (String) principal;
    }
}
