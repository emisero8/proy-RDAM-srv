package ar.gob.rdam.solicitudes.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.common.service.EmailService;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.solicitudes.dto.CrearSolicitudRequest;
import ar.gob.rdam.solicitudes.repository.HistorialEstadoRepository;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SolicitudService — Tests Unitarios")
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private HistorialEstadoRepository historialRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private SolicitudService solicitudService;

    private Usuario ciudadano;
    private Usuario gestor;
    private Solicitud solicitudPendientePago;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(solicitudService, "arancelLibreDeuda", new BigDecimal("1500.00"));
        ReflectionTestUtils.setField(solicitudService, "validezDias", 90);

        ciudadano = Usuario.builder()
                .id(1L).nombre("María").apellido("García").email("maria@test.com")
                .tipo(TipoUsuario.CIUDADANO).rol(RolUsuario.CIUDADANO).activo(true).build();

        gestor = Usuario.builder()
                .id(2L).nombre("Laura").apellido("Martínez").email("laura@rdam.gob.ar")
                .tipo(TipoUsuario.INTERNO).rol(RolUsuario.GESTOR).activo(true).build();

        // En el nuevo flujo, las solicitudes nacen en PENDIENTE_PAGO
        solicitudPendientePago = Solicitud.builder()
                .id(1L).numero("SOL-2026-001")
                .email("maria@test.com").nombre("María").apellido("García").dni("12345678")
                .tipoCert("LIBRE_DEUDA")
                .urgencia("NORMAL")
                .estado(EstadoSolicitud.PENDIENTE_PAGO)
                .arancel(new BigDecimal("1500.00"))
                .build();
    }

    @Test
    @DisplayName("crear: nueva solicitud nace directamente en PENDIENTE_PAGO")
    void crear_debeCrearSolicitudEnEstadoPendientePago() {
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CrearSolicitudRequest req = new CrearSolicitudRequest();
        req.setTipoCert("LIBRE_DEUDA");
        req.setUrgencia("NORMAL");

        var dto = solicitudService.crear(req, "maria@test.com");

        assertNotNull(dto);
        assertEquals(EstadoSolicitud.PENDIENTE_PAGO, dto.getEstado());
        assertEquals(new BigDecimal("1500.00"), dto.getArancel());
        verify(historialRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("cancelar: ciudadano puede cancelar su solicitud desde PENDIENTE_PAGO")
    void cancelar_desdePendientePago_debeTransicionarACancelada() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendientePago));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = solicitudService.cancelar(1L, "maria@test.com");

        assertEquals(EstadoSolicitud.CANCELADA, dto.getEstado());
    }

    @Test
    @DisplayName("cancelar: no se puede cancelar una solicitud ya PAGADA")
    void cancelar_desdePagada_debeLanzarInvalidTransition() {
        solicitudPendientePago.setEstado(EstadoSolicitud.PAGADA);
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendientePago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> solicitudService.cancelar(1L, "maria@test.com"));
        assertEquals("INVALID_TRANSITION", ex.getCode());
    }

    @Test
    @DisplayName("cancelar: ciudadano no puede cancelar solicitud ajena")
    void cancelar_solicitudAjena_debeLanzarForbidden() {
        Usuario otroCiudadano = Usuario.builder()
                .id(99L).nombre("Otro").apellido("Ciudadano").email("otro@test.com")
                .tipo(TipoUsuario.CIUDADANO).rol(RolUsuario.CIUDADANO).activo(true).build();

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendientePago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> solicitudService.cancelar(1L, "otro@test.com"));
        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    @DisplayName("marcarComoPagada: transición PENDIENTE_PAGO → PAGADA es válida")
    void marcarComoPagada_desdePendientePago_debeTransicionarAPagada() {
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        solicitudService.marcarComoPagada(solicitudPendientePago);

        assertEquals(EstadoSolicitud.PAGADA, solicitudPendientePago.getEstado());
        verify(historialRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("getById: ciudadano no puede ver solicitud de otro ciudadano")
    void getById_ciudadanoAjeno_debeLanzarForbidden() {
        Usuario otroCiudadano = Usuario.builder()
                .id(99L).nombre("Otro").apellido("Ciudadano").email("otro@test.com")
                .tipo(TipoUsuario.CIUDADANO).rol(RolUsuario.CIUDADANO).activo(true).build();

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendientePago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> solicitudService.getById(1L, "otro@test.com"));
        assertEquals("FORBIDDEN", ex.getCode());
    }
}
