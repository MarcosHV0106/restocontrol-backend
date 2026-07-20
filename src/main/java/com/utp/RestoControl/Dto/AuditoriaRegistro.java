package com.utp.RestoControl.Dto;

public record AuditoriaRegistro(
        Integer idUsuario,
        String correoUsuario,
        String rolUsuario,
        String modulo,
        String accion,
        String metodoHttp,
        String ruta,
        String recursoId,
        String resultado,
        int estadoHttp,
        long duracionMs,
        String direccionIp,
        String requestId,
        String detalle,
        String tipoError
) {}
