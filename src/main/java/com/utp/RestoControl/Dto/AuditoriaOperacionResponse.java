package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.AuditoriaOperacion;
import java.time.LocalDateTime;

public record AuditoriaOperacionResponse(
        Long idAuditoria,
        LocalDateTime fechaHora,
        Integer idUsuario,
        String correoUsuario,
        String nombreUsuario,
        String rolUsuario,
        String modulo,
        String accion,
        String metodoHttp,
        String ruta,
        String recursoId,
        String resultado,
        Integer estadoHttp,
        Long duracionMs,
        String direccionIp,
        String requestId,
        String detalle,
        String tipoError
) {
    public static AuditoriaOperacionResponse from(AuditoriaOperacion operacion) {
        return new AuditoriaOperacionResponse(
                operacion.getIdAuditoria(), operacion.getFechaHora(), operacion.getIdUsuario(),
                operacion.getCorreoUsuario(), operacion.getNombreUsuario(), operacion.getRolUsuario(),
                operacion.getModulo(), operacion.getAccion(), operacion.getMetodoHttp(), operacion.getRuta(),
                operacion.getRecursoId(), operacion.getResultado(), operacion.getEstadoHttp(),
                operacion.getDuracionMs(), operacion.getDireccionIp(), operacion.getRequestId(),
                operacion.getDetalle(), operacion.getTipoError()
        );
    }
}
