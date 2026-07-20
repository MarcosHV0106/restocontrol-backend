package com.utp.RestoControl.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarDisponibilidadCocinaRequest {

    private Boolean disponible;
    private String motivo;
}
