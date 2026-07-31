package ar.gob.rdam.common.service;

import ar.gob.rdam.domain.enums.EstadoSolicitud;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio centralizado de envío de emails.
 * Todos los envíos son asíncronos para no bloquear el flujo principal.
 * Si falla el envío, se loguea el error sin propagar la excepción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${rdam.mail.from:noreply@rdam.gob.ar}")
    private String fromAddress;

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL DE CÓDIGO DE LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Async
    public void enviarCodigoLogin(String destinatario, String codigo) {
        String asunto = "RDAM — Tu código de verificación";
        String html = """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;
                            background:#ffffff;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden">
                  <div style="background:#1a237e;padding:24px;text-align:center">
                    <h1 style="color:#ffffff;margin:0;font-size:22px">Registro de Deudores Alimentarios Morosos</h1>
                  </div>
                  <div style="padding:32px 28px">
                    <p style="color:#333;font-size:15px;line-height:1.6;margin:0 0 20px">
                      Se solicitó un código de verificación para iniciar sesión en el portal ciudadano.
                    </p>
                    <div style="background:#f5f5f5;border-radius:8px;padding:20px;text-align:center;margin:0 0 20px">
                      <span style="font-size:36px;font-weight:700;letter-spacing:8px;color:#1a237e">%s</span>
                    </div>
                    <p style="color:#666;font-size:13px;line-height:1.5;margin:0 0 8px">
                      Este código es válido por <strong>15 minutos</strong> y es de un solo uso.
                    </p>
                    <p style="color:#999;font-size:12px;margin:0">
                      Si no solicitaste este código, podés ignorar este mensaje.
                    </p>
                  </div>
                  <div style="background:#f9f9f9;padding:14px;text-align:center;border-top:1px solid #e0e0e0">
                    <p style="color:#aaa;font-size:11px;margin:0">RDAM — Sistema de Gestión de Certificados</p>
                  </div>
                </div>
                """.formatted(codigo);

        enviar(destinatario, asunto, html);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EMAIL DE CAMBIO DE ESTADO DE SOLICITUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Async
    public void enviarCambioEstado(String destinatario, String nombreCiudadano,
                                   String numeroSolicitud, EstadoSolicitud estadoAnterior,
                                   EstadoSolicitud estadoNuevo, String comentario) {

        String asuntoTexto = obtenerAsunto(estadoNuevo);
        String asunto = "RDAM — " + asuntoTexto + " (" + numeroSolicitud + ")";

        String estadoColor = obtenerColor(estadoNuevo);
        String estadoLabel = obtenerLabel(estadoNuevo);
        String descripcion = obtenerDescripcion(estadoNuevo, comentario);

        // Sección extra de detalle/motivo solo si hay comentario relevante
        String detalleHtml = "";
        if (comentario != null && !comentario.isBlank() &&
                (estadoNuevo == EstadoSolicitud.RECHAZADA || estadoNuevo == EstadoSolicitud.CANCELADA)) {
            detalleHtml = """
                    <div style="background:#fff3e0;border-left:4px solid #ff9800;padding:12px 16px;
                                border-radius:4px;margin:16px 0">
                      <p style="color:#e65100;font-size:13px;font-weight:600;margin:0 0 4px">Motivo:</p>
                      <p style="color:#333;font-size:14px;margin:0">%s</p>
                    </div>
                    """.formatted(comentario);
        }

        String html = """
                <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:520px;margin:0 auto;
                            background:#ffffff;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden">
                  <div style="background:#1a237e;padding:24px;text-align:center">
                    <h1 style="color:#ffffff;margin:0;font-size:22px">Registro de Deudores Alimentarios Morosos</h1>
                  </div>
                  <div style="padding:32px 28px">
                    <p style="color:#333;font-size:15px;line-height:1.6;margin:0 0 8px">
                      Hola <strong>%s</strong>,
                    </p>
                    <p style="color:#333;font-size:15px;line-height:1.6;margin:0 0 20px">
                      Tu solicitud <strong>%s</strong> cambió de estado:
                    </p>
                    <div style="background:#f5f5f5;border-radius:8px;padding:20px;text-align:center;margin:0 0 16px">
                      <span style="display:inline-block;background:%s;color:#fff;padding:8px 20px;
                                   border-radius:20px;font-size:15px;font-weight:600">%s</span>
                    </div>
                    <p style="color:#555;font-size:14px;line-height:1.5;margin:0 0 8px">%s</p>
                    %s
                    <hr style="border:none;border-top:1px solid #eee;margin:24px 0">
                    <p style="color:#999;font-size:12px;margin:0">
                      Podés consultar el estado de tu solicitud ingresando al portal ciudadano con tu email.
                    </p>
                  </div>
                  <div style="background:#f9f9f9;padding:14px;text-align:center;border-top:1px solid #e0e0e0">
                    <p style="color:#aaa;font-size:11px;margin:0">RDAM — Sistema de Gestión de Certificados</p>
                  </div>
                </div>
                """.formatted(nombreCiudadano, numeroSolicitud, estadoColor,
                estadoLabel, descripcion, detalleHtml);

        enviar(destinatario, asunto, html);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void enviar(String destinatario, String asunto, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("📧 Email enviado a {} — asunto: {}", destinatario, asunto);
        } catch (MessagingException e) {
            log.error("❌ Error al enviar email a {}: {}", destinatario, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar email a {}: {}", destinatario, e.getMessage(), e);
        }
    }

    private String obtenerAsunto(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE_PAGO    -> "Solicitud creada — Pendiente de pago";
            case PAGADA            -> "Pago confirmado";
            case RECHAZADA         -> "Solicitud rechazada";
            case CANCELADA         -> "Solicitud cancelada";
            case EMITIDA           -> "Certificado emitido";
            case EXPIRADA          -> "Certificado expirado";
            default                -> "Actualización de solicitud";
        };
    }

    private String obtenerLabel(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE_PAGO     -> "PENDIENTE DE PAGO";
            case PAGADA             -> "PAGADA";
            case RECHAZADA          -> "RECHAZADA";
            case CANCELADA          -> "CANCELADA";
            case EMITIDA            -> "EMITIDA";
            case EXPIRADA           -> "EXPIRADA";
            default                 -> estado.name();
        };
    }

    private String obtenerColor(EstadoSolicitud estado) {
        return switch (estado) {
            case PENDIENTE_PAGO     -> "#ff9800";
            case PAGADA             -> "#2196f3";
            case RECHAZADA          -> "#f44336";
            case CANCELADA          -> "#9e9e9e";
            case EMITIDA            -> "#4caf50";
            case EXPIRADA           -> "#795548";
            default                 -> "#607d8b";
        };
    }

    private String obtenerDescripcion(EstadoSolicitud estado, String comentario) {
        return switch (estado) {
            case PENDIENTE_PAGO    -> "Tu solicitud fue creada exitosamente. Para continuar con el trámite, es necesario realizar el pago del arancel correspondiente.";
            case PAGADA            -> "Tu pago fue registrado y confirmado exitosamente. Tu solicitud será procesada por un gestor a la brevedad.";
            case RECHAZADA         -> "Lamentablemente, tu solicitud fue rechazada. Consultá el motivo a continuación.";
            case CANCELADA         -> "Tu solicitud fue cancelada.";
            case EMITIDA           -> "¡Tu certificado fue emitido! Ya podés descargarlo desde el portal ciudadano.";
            case EXPIRADA          -> "Tu certificado ha superado su período de validez y se encuentra vencido. Si lo necesitás vigente, podés generar una nueva solicitud.";
            default                -> "El estado de tu solicitud fue actualizado.";
        };
    }
}
