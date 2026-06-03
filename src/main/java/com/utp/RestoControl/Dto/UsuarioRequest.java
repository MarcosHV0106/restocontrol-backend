package com.utp.RestoControl.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.utp.RestoControl.Entity.Rol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequest {

    private String nombre;
    private String apellido;
    private String correo;
    private String clave;

    @JsonAlias({"id_rol", "rolId"})
    private Integer idRol;
    private Rol rol;
}
