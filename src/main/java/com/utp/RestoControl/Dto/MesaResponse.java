package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Mesa;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MesaResponse {

    private Integer idMesa;

    private Integer numeroMesa;

    private Integer capacidad;

    private Integer piso;

    private String estadoMesa;

    public static MesaResponse from(Mesa mesa) {

        return new MesaResponse(
                mesa.getIdMesa(),
                mesa.getNumeroMesa(),
                mesa.getCapacidad(),
                mesa.getPiso(),
                mesa.getEstadoMesa()
        );

    }

}
