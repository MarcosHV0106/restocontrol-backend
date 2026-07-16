package com.utp.RestoControl.Logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.utp.RestoControl.Service.UsuarioService;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ServiceLoggingAspectTests {

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditaEscrituraSinRegistrarArgumentosSensibles() throws Throwable {
        Logger auditLogger = (Logger) LoggerFactory.getLogger("restocontrol.audit");
        Level nivelAnterior = auditLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        auditLogger.setLevel(Level.INFO);
        auditLogger.addAppender(appender);

        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new AnonymousAuthenticationToken(
                            "test-key",
                            "anonymousUser",
                            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
                    )
            );

            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            Signature signature = mock(Signature.class);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.getDeclaringType()).thenReturn(UsuarioService.class);
            when(signature.getName()).thenReturn("cambiarContrasena");
            when(joinPoint.proceed()).thenReturn("ok");

            Object resultado = new ServiceLoggingAspect().registrarServicio(joinPoint);

            assertEquals("ok", resultado);
            assertEquals(1, appender.list.size());
            String mensaje = appender.list.get(0).getFormattedMessage();
            assertTrue(mensaje.contains("evento=UsuarioService.cambiarContrasena"));
            assertTrue(mensaje.contains("resultado=EXITO"));
            assertTrue(mensaje.contains("usuarioId=anonimo"));
            assertFalse(mensaje.contains("clave"));
        } finally {
            auditLogger.detachAppender(appender);
            auditLogger.setLevel(nivelAnterior);
            appender.stop();
        }
    }
}
