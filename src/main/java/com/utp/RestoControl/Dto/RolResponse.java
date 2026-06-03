package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Rol;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RolResponse {

    private Integer idRol;
    private String nombreRol;
    private String descripcion;

    public static RolResponse from(Rol rol) {
        if (rol == null) {
            return null;
        }

        return new RolResponse(
                rol.getIdRol(),
                rol.getNombreRol(),
                rol.getDescripcion()
        );
    }
}
