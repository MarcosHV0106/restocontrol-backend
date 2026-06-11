
package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.ModalidadPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ModalidadPedidoResponse {

    private Integer idModalidaPedido;

    private String nombreModalidad;

    public static ModalidadPedidoResponse from(
            ModalidadPedido modalidadPedido) {

        return new ModalidadPedidoResponse(
                modalidadPedido.getIdModalidadPedido(),
                modalidadPedido.getNombreModalidad()
        );

    }

}
