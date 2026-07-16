package com.utp.RestoControl.Logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger("restocontrol.http");
    private static final Pattern REQUEST_ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern TOKEN_ACTIVACION = Pattern.compile(
            "(?i)(/api/auth/activaciones/)[^/]+"
    );

    @Value("${restocontrol.logging.http.enabled:true}")
    private boolean habilitado;

    @Value("${restocontrol.logging.request-id-header:X-Request-ID}")
    private String requestIdHeader;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !habilitado;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String requestIdAnterior = MDC.get("requestId");
        String requestId = resolverRequestId(request);
        long inicio = System.nanoTime();

        MDC.put("requestId", requestId);
        response.setHeader(nombreHeaderSeguro(), requestId);

        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            long duracionMs = duracionMs(inicio);
            LOGGER.error(
                    "HTTP metodo={} ruta={} resultado=EXCEPCION tipoError={} duracionMs={}",
                    request.getMethod(),
                    sanitizarRuta(request.getRequestURI()),
                    exception.getClass().getSimpleName(),
                    duracionMs
            );
            throw exception;
        } finally {
            registrarRespuesta(request, response, inicio);
            restaurarMdc(requestIdAnterior);
        }
    }

    private void registrarRespuesta(
            HttpServletRequest request,
            HttpServletResponse response,
            long inicio
    ) {
        int estado = response.getStatus();
        String mensaje = "HTTP metodo={} ruta={} estado={} duracionMs={}";
        Object[] argumentos = {
            request.getMethod(),
            sanitizarRuta(request.getRequestURI()),
            estado,
            duracionMs(inicio)
        };

        if (estado >= 500) {
            LOGGER.error(mensaje, argumentos);
        } else if (estado >= 400) {
            LOGGER.warn(mensaje, argumentos);
        } else {
            LOGGER.info(mensaje, argumentos);
        }
    }

    private String resolverRequestId(HttpServletRequest request) {
        String recibido = request.getHeader(nombreHeaderSeguro());
        if (recibido != null && REQUEST_ID_VALIDO.matcher(recibido.trim()).matches()) {
            return recibido.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String nombreHeaderSeguro() {
        return requestIdHeader == null || requestIdHeader.isBlank()
                ? "X-Request-ID"
                : requestIdHeader.trim();
    }

    private String sanitizarRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "/";
        }
        return TOKEN_ACTIVACION.matcher(ruta).replaceAll("$1[REDACTED]");
    }

    private long duracionMs(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    private void restaurarMdc(String requestIdAnterior) {
        if (requestIdAnterior == null) {
            MDC.remove("requestId");
        } else {
            MDC.put("requestId", requestIdAnterior);
        }
    }
}
