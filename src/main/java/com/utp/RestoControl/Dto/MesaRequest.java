package com.utp.RestoControl.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MesaRequest {

    private Integer numeroMesa;

    private Integer capacidad;

    private Integer piso;


}
