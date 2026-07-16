package com.utp.RestoControl.Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.utp.RestoControl.Dto.ApiError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void incluyeRequestIdEnErroresEsperados() {
        MDC.put("requestId", "request-prueba-456");

        ResponseEntity<ApiError> respuesta = handler.manejarNoEncontrado(
                new ResourceNotFoundException("Pedido no encontrado.")
        );

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Pedido no encontrado.", respuesta.getBody().getMensaje());
        assertEquals("request-prueba-456", respuesta.getBody().getRequestId());
    }

    @Test
    void noExponeDetalleDeErroresNoControlados() {
        MDC.put("requestId", "request-error-789");

        ResponseEntity<ApiError> respuesta = handler.manejarErrorNoControlado(
                new RuntimeException("detalle interno sensible")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals(
                "Ocurrio un error interno. Usa el requestId para solicitar soporte.",
                respuesta.getBody().getMensaje()
        );
        assertEquals("request-error-789", respuesta.getBody().getRequestId());
    }
}
