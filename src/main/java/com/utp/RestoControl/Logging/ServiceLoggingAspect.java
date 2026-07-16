package com.utp.RestoControl.Logging;

import com.utp.RestoControl.Security.UserPrincipal;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ServiceLoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoggingAspect.class);
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("restocontrol.audit");
    private static final List<String> PREFIJOS_ESCRITURA = List.of(
            "guardar",
            "actualizar",
            "eliminar",
            "cambiar",
            "reemplazar",
            "cobrar",
            "crear",
            "reenviar",
            "resetear",
            "enviar",
            "procesar"
    );

    @Around("execution(public * com.utp.RestoControl.Service..*(..))")
    public Object registrarServicio(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature firma = joinPoint.getSignature();
        String servicio = firma.getDeclaringType().getSimpleName();
        String operacion = firma.getName();
        String evento = servicio + "." + operacion;
        boolean escritura = esOperacionDeEscritura(operacion);
        long inicio = System.nanoTime();

        LOGGER.debug("Servicio inicio evento={}", evento);
        try {
            Object resultado = joinPoint.proceed();
            long duracionMs = duracionMs(inicio);
            LOGGER.debug("Servicio fin evento={} resultado=EXITO duracionMs={}", evento, duracionMs);

            if (escritura) {
                AUDIT_LOGGER.info(
                        "AUDITORIA evento={} resultado=EXITO usuarioId={} duracionMs={}",
                        evento,
                        resolverUsuarioId(),
                        duracionMs
                );
            }
            return resultado;
        } catch (Throwable exception) {
            long duracionMs = duracionMs(inicio);
            LOGGER.debug(
                    "Servicio fin evento={} resultado=FALLO tipoError={} duracionMs={}",
                    evento,
                    exception.getClass().getSimpleName(),
                    duracionMs
            );

            if (escritura) {
                AUDIT_LOGGER.warn(
                        "AUDITORIA evento={} resultado=FALLO usuarioId={} tipoError={} duracionMs={}",
                        evento,
                        resolverUsuarioId(),
                        exception.getClass().getSimpleName(),
                        duracionMs
                );
            }
            throw exception;
        }
    }

    private boolean esOperacionDeEscritura(String operacion) {
        return PREFIJOS_ESCRITURA.stream().anyMatch(operacion::startsWith);
    }

    private String resolverUsuarioId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "sistema";
        }

        if (authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "anonimo";
        }

        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return String.valueOf(principal.getId());
        }
        return "autenticado";
    }

    private long duracionMs(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}
