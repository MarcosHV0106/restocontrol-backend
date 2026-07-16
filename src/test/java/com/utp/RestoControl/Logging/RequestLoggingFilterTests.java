package com.utp.RestoControl.Logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RequestLoggingFilterTests {

    private RequestLoggingFilter filter;

    @BeforeEach
    void configurarFiltro() {
        filter = new RequestLoggingFilter();
        ReflectionTestUtils.setField(filter, "habilitado", true);
        ReflectionTestUtils.setField(filter, "requestIdHeader", "X-Request-ID");
    }

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void conservaRequestIdValidoYLoDevuelveEnLaRespuesta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/pedidos");
        request.addHeader("X-Request-ID", "frontend-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("requestId", "request-anterior");

        filter.doFilter(request, response, (solicitud, respuesta) -> {
            assertEquals("frontend-123", MDC.get("requestId"));
            ((MockHttpServletResponse) respuesta).setStatus(200);
        });

        assertEquals("frontend-123", response.getHeader("X-Request-ID"));
        assertEquals("request-anterior", MDC.get("requestId"));
    }

    @Test
    void reemplazaRequestIdInvalidoPorUuidSeguro() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/auth/activaciones/token-secreto/crear-clave"
        );
        request.addHeader("X-Request-ID", "valor con espacios y saltos\n");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (solicitud, respuesta) -> {
        });

        String generado = response.getHeader("X-Request-ID");
        assertNotEquals("valor con espacios y saltos\n", generado);
        assertTrue(esUuid(generado));
        assertEquals(null, MDC.get("requestId"));
    }

    private boolean esUuid(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
