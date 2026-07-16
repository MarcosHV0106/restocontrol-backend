package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.DetallePedido;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetallePedidoCocinaResponse {

    private Integer idDetalle;
    private Integer cantidad;
    private String nombreAlimento;

    public static DetallePedidoCocinaResponse from(DetallePedido detalle) {
        return new DetallePedidoCocinaResponse(
                detalle.getIdDetalle(),
                detalle.getCantidad(),
                detalle.getIdAlimento().getNombreAlimento()
        );
    }
}
