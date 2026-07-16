package com.utp.RestoControl.Exception;
import com.utp.RestoControl.Dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> manejarNoEncontrado(ResourceNotFoundException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> manejarCredencialesInvalidas(InvalidCredentialsException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> manejarAccesoDenegado(AccessDeniedException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarSolicitudIncorrecta(IllegalArgumentException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarConflicto(DataIntegrityViolationException exception) {
        return construirRespuesta("No se pudo completar la operacion por restricciones de datos.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> manejarErrorInterno(IllegalStateException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> construirRespuesta(String mensaje, HttpStatus estado) {
        return ResponseEntity
                .status(estado)
                .body(new ApiError(mensaje, estado.value()));
    }
}
