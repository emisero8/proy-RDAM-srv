package ar.gob.rdam.solicitudes.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.entity.Usuario;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.domain.enums.RolUsuario;
import ar.gob.rdam.domain.enums.TipoUsuario;
import ar.gob.rdam.solicitudes.dto.CrearSolicitudRequest;
import ar.gob.rdam.solicitudes.dto.RechazarSolicitudRequest;
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

    @InjectMocks
    private SolicitudService solicitudService;

    private Usuario ciudadano;
    private Usuario gestor;
    private Solicitud solicitudPendiente;

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

        solicitudPendiente = Solicitud.builder()
                .id(1L).numero("SOL-2026-001")
                .ciudadano(ciudadano)
                .tipoCert("LIBRE_DEUDA")
                .urgencia("NORMAL")
                .estado(EstadoSolicitud.PENDIENTE_REVISION)
                .arancel(new BigDecimal("1500.00"))
                .build();
    }

    @Test
    @DisplayName("crear: nueva solicitud se crea en estado PENDIENTE_REVISION")
    void crear_debeCrearSolicitudEnEstadoPendiente() {
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CrearSolicitudRequest req = new CrearSolicitudRequest();
        req.setTipoCert("LIBRE_DEUDA");
        req.setUrgencia("NORMAL");

        var dto = solicitudService.crear(req, ciudadano);

        assertNotNull(dto);
        assertEquals(EstadoSolicitud.PENDIENTE_REVISION, dto.getEstado());
        assertEquals(new BigDecimal("1500.00"), dto.getArancel());
        verify(historialRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("tomar: transición PENDIENTE_REVISION → EN_REVISION es válida")
    void tomar_estadoValido_debeTransicionarAEnRevision() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = solicitudService.tomar(1L, gestor);

        assertEquals(EstadoSolicitud.EN_REVISION, dto.getEstado());
    }

    @Test
    @DisplayName("tomar: transición inválida desde RECHAZADA lanza INVALID_TRANSITION")
    void tomar_desde_rechazada_debeLanzarInvalidTransition() {
        solicitudPendiente.setEstado(EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendiente));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> solicitudService.tomar(1L, gestor));
        assertEquals("INVALID_TRANSITION", ex.getCode());
    }

    @Test
    @DisplayName("rechazar: requiere motivo de rechazo")
    void rechazar_conMotivo_debeRechazarYRegistrarHistorial() {
        solicitudPendiente.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RechazarSolicitudRequest req = new RechazarSolicitudRequest();
        req.setMotivoRechazo("Documentación incompleta");

        var dto = solicitudService.rechazar(1L, req, gestor);

        assertEquals(EstadoSolicitud.RECHAZADA, dto.getEstado());
        assertEquals("Documentación incompleta", dto.getMotivoRechazo());
    }

    @Test
    @DisplayName("aprobar: transición EN_REVISION → APROBADA → PENDIENTE_PAGO (automático)")
    void aprobar_estadoValido_debeTransicionarHastaPendientePago() {
        solicitudPendiente.setEstado(EstadoSolicitud.EN_REVISION);
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendiente));
        when(solicitudRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = solicitudService.aprobar(1L, "Todo correcto", gestor);

        assertEquals(EstadoSolicitud.PENDIENTE_PAGO, dto.getEstado());
    }

    @Test
    @DisplayName("getById: ciudadano no puede ver solicitud de otro ciudadano")
    void getById_ciudadanoAjeno_debeLanzarForbidden() {
        Usuario otroCiudadano = Usuario.builder()
                .id(99L).nombre("Otro").apellido("Ciudadano").email("otro@test.com")
                .tipo(TipoUsuario.CIUDADANO).rol(RolUsuario.CIUDADANO).activo(true).build();

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendiente));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> solicitudService.getById(1L, otroCiudadano));
        assertEquals("FORBIDDEN", ex.getCode());
    }
}
