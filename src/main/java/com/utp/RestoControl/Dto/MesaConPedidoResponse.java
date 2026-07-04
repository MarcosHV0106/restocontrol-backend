package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.EstadoMesa;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.Pedido;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MesaConPedidoResponse {

    private Integer idMesa;
    private Integer numeroMesa;
    private Integer capacidad;
    private Integer piso;
    private EstadoMesa estadoMesa;
    private PedidoMesaResponse pedido;

    public static MesaConPedidoResponse from(Mesa mesa, Pedido pedido) {
        return new MesaConPedidoResponse(
                mesa.getIdMesa(),
                mesa.getNumeroMesa(),
                mesa.getCapacidad(),
                mesa.getPiso(),
                mesa.getEstadoMesa(),
                PedidoMesaResponse.from(pedido)
        );
    }
}
