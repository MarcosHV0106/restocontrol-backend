package com.utp.RestoControl.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarClaveRequest {

    private String claveActual;
    private String claveNueva;
    private String confirmarClave;

}
