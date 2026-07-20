package com.utp.RestoControl.Logging;

import com.utp.RestoControl.Dto.AuditoriaRegistro;
import com.utp.RestoControl.Security.UserPrincipal;
import com.utp.RestoControl.Service.AuditoriaOperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditoriaOperacionInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditoriaOperacionInterceptor.class);
    private static final String ATRIBUTO_INICIO = AuditoriaOperacionInterceptor.class.getName() + ".inicio";
    private static final String ATRIBUTO_IDENTIDAD = AuditoriaOperacionInterceptor.class.getName() + ".identidad";
    private static final Set<String> METODOS_AUDITABLES = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern ID_RECURSO = Pattern.compile("/(\\d+)(?:/|$)");
    private static final Pattern TOKEN_ACTIVACION = Pattern.compile("(?i)(/api/auth/activaciones/)[^/]+");

    private final AuditoriaOperacionService service;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (esAuditable(request)) {
            request.setAttribute(ATRIBUTO_INICIO, System.nanoTime());
            request.setAttribute(ATRIBUTO_IDENTIDAD, resolverIdentidad());
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (!esAuditable(request)) {
            return;
        }

        try {
            String ruta = sanitizarRuta(request.getRequestURI());
            Identidad identidad = resolverIdentidadFinal(request);
            int estado = response.getStatus();
            String resultado = exception == null && estado < 400 ? "EXITO" : "FALLO";
            long duracionMs = resolverDuracion(request);
            String tipoError = exception == null ? null : exception.getClass().getSimpleName();

            service.registrarOperacion(new AuditoriaRegistro(
                    identidad.idUsuario(),
                    identidad.correo(),
                    identidad.rol(),
                    resolverModulo(ruta),
                    resolverAccion(request.getMethod(), ruta),
                    request.getMethod(),
                    ruta,
                    resolverIdRecurso(ruta),
                    resultado,
                    estado,
                    duracionMs,
                    resolverIp(request),
                    response.getHeader("X-Request-ID"),
                    request.getMethod() + " " + ruta + " -> HTTP " + estado,
                    tipoError
            ));
        } catch (RuntimeException auditoriaException) {
            LOGGER.warn(
                    "No se pudo persistir la auditoria metodo={} ruta={} tipoError={}",
                    request.getMethod(),
                    sanitizarRuta(request.getRequestURI()),
                    auditoriaException.getClass().getSimpleName()
            );
        }
    }

    private boolean esAuditable(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta != null
                && ruta.startsWith("/api/")
                && !ruta.startsWith("/api/auditoria")
                && METODOS_AUDITABLES.contains(request.getMethod().toUpperCase(Locale.ROOT));
    }

    private Identidad resolverIdentidad() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new Identidad(null, "sistema", "SISTEMA");
        }

        String rol = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse("AUTENTICADO");
        if (authentication.getPrincipal() instanceof UserPrincipal principal) {
            return new Identidad(principal.getId(), principal.getUsername(), rol);
        }
        return new Identidad(null, authentication.getName(), rol);
    }

    private Identidad resolverIdentidadFinal(HttpServletRequest request) {
        Identidad posterior = resolverIdentidad();
        if (posterior.idUsuario() != null) {
            return posterior;
        }
        Object identidadInicial = request.getAttribute(ATRIBUTO_IDENTIDAD);
        return identidadInicial instanceof Identidad identidad ? identidad : posterior;
    }

    private String resolverModulo(String ruta) {
        if (contiene(ruta, "/usuarios", "/roles", "/auth")) return "USUARIOS";
        if (contiene(ruta, "/alimentos", "/categorias", "/alimento-insumo")) return "MENU";
        if (contiene(ruta, "/pedidos", "/mesas", "/modalidadespedidos")) return "PEDIDOS";
        if (ruta.contains("/cocina")) return "COCINA";
        if (ruta.contains("/cobros")) return "CAJA";
        if (contiene(ruta, "/insumos", "/lotes", "/movimientosinventarios")) return "INVENTARIO";
        if (ruta.contains("/alertas")) return "ALERTAS";
        if (ruta.contains("/estimaciones-diarias")) return "ESTIMACION_DIARIA";
        if (contiene(ruta, "/proveedores", "/compras-abastecimiento")) return "ABASTECIMIENTO";
        if (ruta.contains("/reportes")) return "REPORTES";
        return "GENERAL";
    }

    private String resolverAccion(String metodo, String ruta) {
        if (ruta.contains("/login")) return "INICIAR_SESION";
        if (ruta.contains("/reenviar-activacion")) return "REENVIAR_ACTIVACION";
        if (ruta.contains("/cambiar-contrasena")) return "CAMBIAR_CONTRASENA";
        if (ruta.contains("/enviar-cocina")) return "ENVIAR_COCINA";
        if (ruta.contains("/anular")) return "ANULAR_PEDIDO";
        if (ruta.contains("/solicitar-cuenta")) return "SOLICITAR_CUENTA";
        if (ruta.contains("/transferir")) return "TRANSFERIR_ATENCION";
        if (ruta.contains("/cambiar-mesa")) return "CAMBIAR_MESA";
        if (ruta.contains("/estado")) return "CAMBIAR_ESTADO";
        if (ruta.contains("/disponibilidad")) return "CAMBIAR_DISPONIBILIDAD";
        if (ruta.contains("/ajust")) return "AJUSTAR_INVENTARIO";
        if (ruta.contains("/retirar")) return "REGISTRAR_SALIDA";
        if (ruta.contains("/cobrar") || ruta.contains("/pagos")) return "REGISTRAR_PAGO";
        if (ruta.contains("/receta")) return "CONFIGURAR_RECETA";
        if ("POST".equalsIgnoreCase(metodo)) return "REGISTRAR";
        if ("DELETE".equalsIgnoreCase(metodo)) return "DESACTIVAR";
        return "ACTUALIZAR";
    }

    private String resolverIdRecurso(String ruta) {
        Matcher matcher = ID_RECURSO.matcher(ruta);
        String ultimoId = null;
        while (matcher.find()) {
            ultimoId = matcher.group(1);
        }
        return ultimoId;
    }

    private String resolverIp(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        String ip = reenviada == null || reenviada.isBlank()
                ? request.getRemoteAddr()
                : reenviada.split(",", 2)[0].trim();
        return ip != null && ip.length() > 64 ? ip.substring(0, 64) : ip;
    }

    private long resolverDuracion(HttpServletRequest request) {
        Object inicio = request.getAttribute(ATRIBUTO_INICIO);
        return inicio instanceof Long valor ? Math.max(0, (System.nanoTime() - valor) / 1_000_000) : 0;
    }

    private String sanitizarRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return "/";
        }
        return TOKEN_ACTIVACION.matcher(ruta).replaceAll("$1[REDACTED]");
    }

    private boolean contiene(String valor, String... fragmentos) {
        for (String fragmento : fragmentos) {
            if (valor.contains(fragmento)) return true;
        }
        return false;
    }

    private record Identidad(Integer idUsuario, String correo, String rol) {}
}
