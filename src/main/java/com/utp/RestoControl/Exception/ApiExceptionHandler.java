package com.utp.RestoControl.Exception;

import com.utp.RestoControl.Dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(ResourceNotFoundException exception) {
        return construirRespuestaEsperada(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> manejarCredencialesInvalidas(InvalidCredentialsException exception) {
        return construirRespuestaEsperada(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> manejarAccesoDenegado(AccessDeniedException exception) {
        return construirRespuestaEsperada(exception, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarSolicitudIncorrecta(IllegalArgumentException exception) {
        return construirRespuestaEsperada(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> manejarJsonInvalido(HttpMessageNotReadableException exception) {
        LOGGER.warn("Solicitud rechazada tipo=JSON_INVALIDO estado={}", HttpStatus.BAD_REQUEST.value());
        return construirRespuesta("El cuerpo de la solicitud no tiene un formato valido.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarConflicto(DataIntegrityViolationException exception) {
        LOGGER.warn(
                "Solicitud rechazada tipo=INTEGRIDAD_DATOS estado={} causa={}",
                HttpStatus.CONFLICT.value(),
                causaPrincipal(exception).getClass().getSimpleName()
        );
        return construirRespuesta(
                "No se pudo completar la operacion por restricciones de datos.",
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> manejarErrorInterno(IllegalStateException exception) {
        LOGGER.error("Error interno controlado tipo={}", exception.getClass().getSimpleName(), exception);
        return construirRespuesta(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> manejarErrorNoControlado(Exception exception) {
        LOGGER.error("Error interno no controlado tipo={}", exception.getClass().getSimpleName(), exception);
        return construirRespuesta(
                "Ocurrio un error interno. Usa el requestId para solicitar soporte.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    private ResponseEntity<ApiError> construirRespuestaEsperada(
            RuntimeException exception,
            HttpStatus estado
    ) {
        LOGGER.warn(
                "Solicitud rechazada tipo={} estado={} mensaje={}",
                exception.getClass().getSimpleName(),
                estado.value(),
                exception.getMessage()
        );
        return construirRespuesta(exception.getMessage(), estado);
    }

    private ResponseEntity<ApiError> construirRespuesta(String mensaje, HttpStatus estado) {
        return ResponseEntity
                .status(estado)
                .body(new ApiError(mensaje, estado.value(), MDC.get("requestId")));
    }

    private Throwable causaPrincipal(Throwable exception) {
        Throwable causa = exception;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa;
    }
}
