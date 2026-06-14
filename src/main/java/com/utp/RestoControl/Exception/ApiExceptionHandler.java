package com.utp.RestoControl.Exception;
import com.utp.RestoControl.Dto.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> manejarSolicitudIncorrecta(IllegalArgumentException exception) {
        return construirRespuesta(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> manejarConflicto(DataIntegrityViolationException exception) {
        return construirRespuesta("No se pudo completar la operacion por restricciones de datos.", HttpStatus.CONFLICT);
    }

    private ResponseEntity<ApiError> construirRespuesta(String mensaje, HttpStatus estado) {
        return ResponseEntity
                .status(estado)
                .body(new ApiError(mensaje, estado.value()));
    }
}
