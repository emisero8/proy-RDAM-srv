package ar.gob.rdam.config;

import ar.gob.rdam.auth.filter.JwtAuthFilter;
import ar.gob.rdam.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UsuarioRepository usuarioRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                            authException.getMessage());
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ─── Rutas públicas ──────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/solicitar-codigo",
                                "/auth/validar-codigo",
                                "/auth/admin/login",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/pagos/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/certificados/*/verificar").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // ─── Perfil propio (cualquier usuario autenticado) ──
                        .requestMatchers("/usuarios/me", "/usuarios/me/**").authenticated()

                        // ─── Solo ADMIN ──────────────────────────────────────
                        .requestMatchers("/usuarios", "/usuarios/**").hasAnyRole("ADMIN")
                        .requestMatchers("/reportes/**").hasRole("ADMIN")

                        // ─── GESTOR o ADMIN ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/solicitudes").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/solicitudes/*/tomar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/solicitudes/*/aprobar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/solicitudes/*/rechazar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/certificados/emitir").hasAnyRole("GESTOR", "ADMIN")

                        // ─── Solo CIUDADANO ──────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/solicitudes/mis").hasRole("CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/solicitudes").hasRole("CIUDADANO")
                        .requestMatchers(HttpMethod.POST, "/pagos/iniciar").hasRole("CIUDADANO")

                        // ─── Cualquier usuario autenticado ───────────────────
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.rdam.gob.ar"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
