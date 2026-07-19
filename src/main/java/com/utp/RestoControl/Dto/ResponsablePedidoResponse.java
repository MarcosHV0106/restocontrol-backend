package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponsablePedidoResponse {

    private Integer idUsuario;
    private String nombreCompleto;
    private String correo;

    public static ResponsablePedidoResponse from(Usuario usuario) {
        return new ResponsablePedidoResponse(
                usuario.getIdUsuario(),
                (usuario.getNombre() + " " + usuario.getApellido()).trim(),
                usuario.getCorreo()
        );
    }
}
