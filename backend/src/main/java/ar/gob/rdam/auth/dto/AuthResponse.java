package ar.gob.rdam.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String portal;
    private UsuarioInfo usuario;

    @Data
    @Builder
    public static class UsuarioInfo {
        private Long id;
        private String nombre;
        private String apellido;
        private String email;
        private String rol;
    }
}
