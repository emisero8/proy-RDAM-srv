package ar.gob.rdam.usuarios.controller;

import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.usuarios.dto.CrearUsuarioInternoRequest;
import ar.gob.rdam.usuarios.dto.UsuarioDTO;
import ar.gob.rdam.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** GET /usuarios — Listar todos los usuarios internos (ADMIN) */
    @GetMapping
    public ResponseEntity<Page<UsuarioDTO>> listar(
            @RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(usuarioService.listar(rol, activo, search,
                PageRequest.of(page - 1, limit, Sort.by(dir, sort))));
    }

    /** GET /usuarios/me — Perfil propio */
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> me(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.getMe(usuario));
    }

    /** GET /usuarios/:id */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.getById(id));
    }

    /** POST /usuarios — Crear usuario interno (ADMIN) */
    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody CrearUsuarioInternoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearInterno(request));
    }

    /** PATCH /usuarios/:id/rol */
    @PatchMapping("/{id}/rol")
    public ResponseEntity<UsuarioDTO> cambiarRol(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        RolUsuario nuevoRol = RolUsuario.valueOf(body.get("rol"));
        return ResponseEntity.ok(usuarioService.cambiarRol(id, nuevoRol));
    }

    /** PUT /usuarios/:id — Editar datos del usuario (ADMIN) */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        RolUsuario rol = body.get("rol") != null ? RolUsuario.valueOf(body.get("rol")) : null;
        return ResponseEntity.ok(usuarioService.actualizar(id,
                body.get("nombre"), body.get("apellido"), body.get("email"),
                body.get("dniCuil"), body.get("telefono"), rol));
    }

    /** PATCH /usuarios/:id/estado */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<UsuarioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, body.get("activo")));
    }

    /** DELETE /usuarios/:id — Soft delete */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
