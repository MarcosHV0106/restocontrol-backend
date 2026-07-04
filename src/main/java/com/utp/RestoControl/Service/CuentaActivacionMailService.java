package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Usuario;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CuentaActivacionMailService {

    private final JavaMailSender mailSender;

    @Value("${restocontrol.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${restocontrol.mail-from-name:RestoControl Notificaciones}")
    private String mailFromName;

    public void enviarInvitacion(Usuario usuario, String tokenPlano) {
        String enlace = construirEnlace(tokenPlano);

        try {
            validarRemitenteConfigurado();

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(mailFrom.trim(), mailFromName.trim());

            helper.setTo(usuario.getCorreo());
            helper.setSubject("Activa tu cuenta de RestoControl");
            helper.setText(construirHtml(usuario, enlace), true);

            mailSender.send(mensaje);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("No se pudo preparar el correo de activacion.", ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("No se pudo enviar el correo de activacion.", ex);
        }
    }

    private void validarRemitenteConfigurado() {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("El remitente de correo no esta configurado. Revisa MAIL_USERNAME.");
        }
    }

    private String construirEnlace(String tokenPlano) {
        String base = frontendUrl == null || frontendUrl.isBlank()
                ? "http://localhost:5173"
                : frontendUrl.trim();

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + "/activar-cuenta/" + tokenPlano;
    }

    private String construirHtml(Usuario usuario, String enlace) {
        String nombre = escapeHtml(usuario.getNombre());
        String enlaceSeguro = escapeHtml(enlace);

        return """
                <div style="font-family: Arial, sans-serif; color: #2c1a11; line-height: 1.5;">
                    <h2 style="margin: 0 0 12px; color: #df7a48;">Bienvenido a RestoControl</h2>
                    <p>Hola %s,</p>
                    <p>Se creo una cuenta para ti. Para activarla, crea tu contrasena desde el siguiente enlace:</p>
                    <p style="margin: 24px 0;">
                        <a href="%s" style="background: #df7a48; color: #ffffff; padding: 12px 18px; border-radius: 8px; text-decoration: none; font-weight: 700;">
                            Crear mi contrasena
                        </a>
                    </p>
                    <p>Si el boton no funciona, copia y pega este enlace en tu navegador:</p>
                    <p style="word-break: break-all; color: #7f8c8d;">%s</p>
                    <p>Este enlace vence pronto por seguridad.</p>
                </div>
                """.formatted(nombre, enlaceSeguro, enlaceSeguro);
    }

    private String escapeHtml(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
