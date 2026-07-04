package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.DetallePedido;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetallePedidoMesaResponse {

    private Integer idDetalle;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private AlimentoMesaResponse alimento;

    public static DetallePedidoMesaResponse from(DetallePedido detalle) {
        return new DetallePedidoMesaResponse(
                detalle.getIdDetalle(),
                detalle.getCantidad(),
                detalle.getPrecio_unitario(),
                detalle.getSubtotal(),
                AlimentoMesaResponse.from(detalle.getIdAlimento())
        );
    }
}
