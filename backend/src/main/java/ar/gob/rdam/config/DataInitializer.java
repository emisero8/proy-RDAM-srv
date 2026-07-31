package ar.gob.rdam.config;

import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea datos de prueba al arrancar en perfil 'dev' o 'test'.
 * En producción NO se ejecuta.
 *
 * NOTA: Los ciudadanos ya NO se almacenan en la base de datos.
 * Solo se persisten usuarios INTERNOS (gestores, admin).
 */
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

        private final UsuarioRepository usuarioRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(ApplicationArguments args) {
                if (usuarioRepository.count() > 0) {
                        log.info("ℹ️  DataInitializer: ya existen usuarios, se omite la carga inicial.");
                        return;
                }

                // Gestor 1 — Circunscripción I
                usuarioRepository.save(Usuario.builder()
                                .nombre("Laura")
                                .apellido("Martínez")
                                .email("lmartinez@rdam.gob.ar")
                                .password(passwordEncoder.encode("Password1!"))
                                .dniCuil("27-12345678-9")
                                .tipo(TipoUsuario.INTERNO)
                                .rol(RolUsuario.GESTOR)
                                .circunscripcion("Circunscripción I")
                                .activo(true)
                                .build());

                // Gestor 2 — Circunscripción II
                usuarioRepository.save(Usuario.builder()
                                .nombre("Pedro")
                                .apellido("Rodríguez")
                                .email("prodriguez@rdam.gob.ar")
                                .password(passwordEncoder.encode("Password1!"))
                                .dniCuil("20-98765432-1")
                                .tipo(TipoUsuario.INTERNO)
                                .rol(RolUsuario.GESTOR)
                                .circunscripcion("Circunscripción II")
                                .activo(true)
                                .build());

                // Admin — Todas las circunscripciones
                usuarioRepository.save(Usuario.builder()
                                .nombre("Carlos")
                                .apellido("Admin")
                                .email("admin@rdam.gob.ar")
                                .password(passwordEncoder.encode("Password1!"))
                                .dniCuil("20-11111111-1")
                                .tipo(TipoUsuario.INTERNO)
                                .rol(RolUsuario.ADMIN)
                                .circunscripcion("TODAS")
                                .activo(true)
                                .build());

                log.info("✅ DataInitializer: usuarios internos de prueba creados (gestores + admin).");
        }
}

