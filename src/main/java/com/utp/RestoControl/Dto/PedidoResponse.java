
package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
public class PedidoResponse {
    private Integer idPedido;

    private LocalDateTime fechaPedido;

    private BigDecimal total;

    private String observacion;

    //private MesaResponse mesa;

    private UsuarioResponse usuario;

    //private EstadoPedidoResponse estadoPedido;

    //private ModalidadPedidoResponse modalidadPedido;
/*
    public static PedidoResponse from(Pedido pedido){

        return new PedidoResponse(

                pedido.getIdPedido(),

                pedido.getFechaPedido(),

                pedido.getTotal(),

                pedido.getObservacion(),

                MesaResponse.from(
                        pedido.getIdMesa()
                ),

                UsuarioResponse.from(
                        pedido.getIdUsuario()
                ),

                EstadoPedidoResponse.from(
                        pedido.getEstadoPedido()
                ),

                ModalidadPedidoResponse.from(
                        pedido.getModalidadPedido()
                )

        );

    }*/

}
