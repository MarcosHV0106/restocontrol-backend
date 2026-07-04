package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PedidoMesaResponse {

    private Integer idPedido;
    private BigDecimal total;
    private List<DetallePedidoMesaResponse> detalles;

    public static PedidoMesaResponse from(Pedido pedido) {
        if (pedido == null) {
            return null;
        }

        return new PedidoMesaResponse(
                pedido.getIdPedido(),
                pedido.getTotal(),
                (pedido.getDetalles() == null ? Collections.<DetallePedido>emptyList() : pedido.getDetalles())
                        .stream()
                        .map(DetallePedidoMesaResponse::from)
                        .toList()
        );
    }
}
