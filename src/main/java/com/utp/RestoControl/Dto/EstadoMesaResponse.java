package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.EstadoMesa;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstadoMesaResponse {

    private Integer idEstadoMesa;

    private String descripcion;

    public static EstadoMesaResponse from(
            EstadoMesa estadoMesa) {

        return new EstadoMesaResponse(
                estadoMesa.getIdEstadoMesa(),
                estadoMesa.getDescripcion()
        );

    }

}
