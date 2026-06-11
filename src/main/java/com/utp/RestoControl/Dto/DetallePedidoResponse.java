/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Dto;
import com.utp.RestoControl.Entity.DetallePedido;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DetallePedidoResponse {

    private Integer idDetalle;

    private Integer cantidad;

    private BigDecimal precioUnitario;

    private BigDecimal subtotal;

    private AlimentoResponse alimento;

    public static DetallePedidoResponse from(
            DetallePedido detalle) {

        return new DetallePedidoResponse(

                detalle.getIdDetalle(),

                detalle.getCantidad(),

                detalle.getPrecio_unitario(),

                detalle.getSubtotal(),

                AlimentoResponse.from(
                        detalle.getIdAlimento()
                )

        );

    }

}