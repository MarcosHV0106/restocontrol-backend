package com.utp.RestoControl.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProveedorRequest {
    private String razonSocial;
    private String ruc;
    private String contacto;
    private String telefono;
    private String correo;
    private String direccion;
    private Boolean activo;
}
