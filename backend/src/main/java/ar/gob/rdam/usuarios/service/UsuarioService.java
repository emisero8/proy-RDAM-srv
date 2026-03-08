package ar.gob.rdam.usuarios.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.usuarios.dto.CrearUsuarioInternoRequest;
import ar.gob.rdam.usuarios.dto.UsuarioDTO;
import ar.gob.rdam.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Listado (solo ADMIN) ─────────────────────────────────────────────────

    public Page<UsuarioDTO> listar(RolUsuario rol, Boolean activo, String search, Pageable pageable) {
        int activoInt = activo == null ? -1 : (activo ? 1 : 0);
        String searchStr = search != null ? search.toLowerCase() : "";

        if (rol == null) {
            return usuarioRepository.buscarUsuariosSinRol(activoInt, searchStr, pageable)
                    .map(this::toDTO);
        } else {
            return usuarioRepository.buscarUsuariosConRol(rol, activoInt, searchStr, pageable)
                    .map(this::toDTO);
        }
    }

    // ─── Ver perfil ───────────────────────────────────────────────────────────

    public UsuarioDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    public UsuarioDTO getMe(Usuario usuario) {
        return toDTO(usuario);
    }

    // ─── Crear usuario interno ────────────────────────────────────────────────

    @Transactional
    public UsuarioDTO crearInterno(CrearUsuarioInternoRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("CONFLICT", "El email ya está registrado.");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .dniCuil(request.getDniCuil())
                .telefono(request.getTelefono())
                .tipo(TipoUsuario.INTERNO)
                .rol(request.getRol())
                .activo(true)
                .build();

        return toDTO(usuarioRepository.save(usuario));
    }

    // ─── Activar / Desactivar ─────────────────────────────────────────────────

    @Transactional
    public UsuarioDTO cambiarEstado(Long id, boolean activo) {
        Usuario u = findOrThrow(id);
        u.setActivo(activo);
        return toDTO(usuarioRepository.save(u));
    }

    // ─── Cambiar Rol ──────────────────────────────────────────────────────────

    @Transactional
    public UsuarioDTO cambiarRol(Long id, RolUsuario nuevoRol) {
        Usuario u = findOrThrow(id);
        if (u.getTipo() == TipoUsuario.CIUDADANO) {
            throw new BusinessException("BUSINESS_RULE", "No se puede cambiar el rol de un ciudadano.");
        }
        u.setRol(nuevoRol);
        return toDTO(usuarioRepository.save(u));
    }

    // ─── Actualizar datos de usuario (ADMIN) ──────────────────────────────────

    @Transactional
    public UsuarioDTO actualizar(Long id, String nombre, String apellido, String email,
            String dniCuil, String telefono, RolUsuario rol) {
        Usuario u = findOrThrow(id);
        if (nombre != null && !nombre.isBlank())
            u.setNombre(nombre);
        if (apellido != null && !apellido.isBlank())
            u.setApellido(apellido);
        if (email != null && !email.isBlank()) {
            if (!email.equals(u.getEmail()) && usuarioRepository.existsByEmail(email)) {
                throw new BusinessException("CONFLICT", "El email ya está registrado.");
            }
            u.setEmail(email);
        }
        if (dniCuil != null)
            u.setDniCuil(dniCuil);
        if (telefono != null)
            u.setTelefono(telefono);
        if (rol != null && u.getTipo() != TipoUsuario.CIUDADANO) {
            u.setRol(rol);
        }
        return toDTO(usuarioRepository.save(u));
    }

    // ─── Eliminar (soft delete) ───────────────────────────────────────────────

    @Transactional
    public void eliminar(Long id) {
        Usuario u = findOrThrow(id);
        u.setActivo(false);
        usuarioRepository.save(u);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Usuario findOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Usuario no encontrado."));
    }

    public UsuarioDTO toDTO(Usuario u) {
        return UsuarioDTO.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .email(u.getEmail())
                .dniCuil(u.getDniCuil())
                .telefono(u.getTelefono())
                .domicilio(u.getDomicilio())
                .tipo(u.getTipo())
                .rol(u.getRol())
                .activo(u.getActivo())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
