package com.utp.RestoControl.Dto;

import java.util.List;

public record AuditoriaOpcionesResponse(
        List<String> modulos,
        List<String> acciones,
        List<UsuarioAuditoria> usuarios
) {
    public record UsuarioAuditoria(
            Integer idUsuario,
            String nombre,
            String correo,
            String rol
    ) {}
}
