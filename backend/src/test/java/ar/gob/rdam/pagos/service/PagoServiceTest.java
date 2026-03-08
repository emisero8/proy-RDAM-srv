package ar.gob.rdam.pagos.service;

import ar.gob.rdam.common.exception.BusinessException;
import ar.gob.rdam.domain.entity.Pago;
import ar.gob.rdam.domain.entity.Solicitud;
import ar.gob.rdam.domain.enums.EstadoPago;
import ar.gob.rdam.domain.enums.EstadoSolicitud;
import ar.gob.rdam.pagos.dto.IniciarPagoRequest;
import ar.gob.rdam.pagos.dto.WebhookPlusPagosRequest;
import ar.gob.rdam.pagos.repository.PagoRepository;
import ar.gob.rdam.solicitudes.repository.SolicitudRepository;
import ar.gob.rdam.solicitudes.service.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService — Tests Unitarios")
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private SolicitudService solicitudService;
    @Mock
    private PlusPagosEncryptionService encryptionService;

    @InjectMocks
    private PagoService pagoService;

    private Solicitud solicitudPendientePago;
    private Solicitud solicitudEnRevision;
    private Pago pagoPendiente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "plusPagosApiUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(pagoService, "merchantGuid", "test-merchant-001");
        ReflectionTestUtils.setField(pagoService, "secretKey", "clave-secreta-campus-2026");
        ReflectionTestUtils.setField(pagoService, "frontendUrl", "http://localhost:5173");

        // Stub encriptación — devuelve el texto tal cual (suficiente para tests
        // unitarios)
        lenient().when(encryptionService.encrypt(any(), any())).thenAnswer(i -> "ENC:" + i.getArgument(0));

        solicitudPendientePago = Solicitud.builder()
                .id(1L).numero("SOL-2026-001")
                .estado(EstadoSolicitud.PENDIENTE_PAGO)
                .arancel(new BigDecimal("1500.00"))
                .build();

        solicitudEnRevision = Solicitud.builder()
                .id(2L).numero("SOL-2026-002")
                .estado(EstadoSolicitud.EN_REVISION)
                .arancel(new BigDecimal("1500.00"))
                .build();

        pagoPendiente = Pago.builder()
                .id(1L)
                .solicitud(solicitudPendientePago)
                .monto(new BigDecimal("1500.00"))
                .checkoutUrl("http://localhost:3000")
                .estado(EstadoPago.PENDIENTE)
                .build();
    }

    // ─── UT-P01 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("iniciarPago: solicitud PENDIENTE_PAGO crea pago con campos encriptados")
    void iniciarPago_solicitudPendientePago_debeCrearPago() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudPendientePago));
        when(pagoRepository.findBySolicitudId(1L)).thenReturn(Optional.empty());
        when(pagoRepository.save(any())).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        IniciarPagoRequest req = new IniciarPagoRequest();
        req.setSolicitudId(1L);

        var dto = pagoService.iniciarPago(req);

        assertNotNull(dto);
        assertNotNull(dto.getMontoEnc());
        assertNotNull(dto.getPlusPagosUrl());
        assertEquals("test-merchant-001", dto.getComercio());
        assertEquals(new BigDecimal("1500.00"), dto.getMonto());
        assertEquals(EstadoPago.PENDIENTE, dto.getEstado());
        verify(pagoRepository, times(1)).save(any());
    }

    // ─── UT-P02 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("iniciarPago: solicitud NO PENDIENTE_PAGO lanza BUSINESS_RULE")
    void iniciarPago_solicitudNoAprobada_debeLanzarBusinessRule() {
        when(solicitudRepository.findById(2L)).thenReturn(Optional.of(solicitudEnRevision));

        IniciarPagoRequest req = new IniciarPagoRequest();
        req.setSolicitudId(2L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.iniciarPago(req));
        assertEquals("BUSINESS_RULE", ex.getCode());
    }

    // ─── UT-P03 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("procesarWebhook: estado REALIZADA marca solicitud como PAGADA")
    void procesarWebhook_realizada_debeMarcarComoPagada() {
        when(solicitudRepository.findAll()).thenReturn(List.of(solicitudPendientePago));
        when(pagoRepository.findBySolicitudId(1L)).thenReturn(Optional.of(pagoPendiente));

        WebhookPlusPagosRequest payload = new WebhookPlusPagosRequest();
        payload.setTransaccionComercioId("TXN-SOL-2026-001-1709559430000");
        payload.setEstado("REALIZADA");
        payload.setTransaccionPlataformaId("PP-123456");
        payload.setMonto("1500.00");

        pagoService.procesarWebhook(payload);

        assertEquals(EstadoPago.APROBADO, pagoPendiente.getEstado());
        assertEquals("PP-123456", pagoPendiente.getReferencia());
        verify(pagoRepository, times(1)).save(pagoPendiente);
        verify(solicitudService, times(1)).marcarComoPagada(solicitudPendientePago);
    }

    // ─── UT-P04 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("procesarWebhook: estado RECHAZADA cambia pago a RECHAZADO sin modificar solicitud")
    void procesarWebhook_rechazada_debeRechazarPagoSinCambiarSolicitud() {
        when(solicitudRepository.findAll()).thenReturn(List.of(solicitudPendientePago));
        when(pagoRepository.findBySolicitudId(1L)).thenReturn(Optional.of(pagoPendiente));

        WebhookPlusPagosRequest payload = new WebhookPlusPagosRequest();
        payload.setTransaccionComercioId("TXN-SOL-2026-001-1709559430000");
        payload.setEstado("RECHAZADA");
        payload.setMonto("1500.00");

        pagoService.procesarWebhook(payload);

        assertEquals(EstadoPago.RECHAZADO, pagoPendiente.getEstado());
        verify(pagoRepository, times(1)).save(pagoPendiente);
        verify(solicitudService, never()).marcarComoPagada(any());
    }

    // ─── UT-P05 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("procesarWebhook: referencia inexistente lanza NOT_FOUND")
    void procesarWebhook_referenciaInexistente_debeLanzarNotFound() {
        when(solicitudRepository.findAll()).thenReturn(List.of());

        WebhookPlusPagosRequest payload = new WebhookPlusPagosRequest();
        payload.setTransaccionComercioId("TXN-SOL-9999-999-1709559430000");
        payload.setEstado("REALIZADA");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.procesarWebhook(payload));
        assertEquals("NOT_FOUND", ex.getCode());
    }
}
