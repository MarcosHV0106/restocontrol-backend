package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstadoPedidoResponse {

    private Integer idEstadoPedido;

    private String nombreEstado;

    public static EstadoPedidoResponse from(
            EstadoPedido estadoPedido) {

        return new EstadoPedidoResponse(
                estadoPedido.getIdEstadoPedido(),
                estadoPedido.getNombreEstado()
        );

    }

}
