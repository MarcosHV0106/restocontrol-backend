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
    private String etapaFlujo;
    private boolean editable;
    private boolean puedeEnviarCocina;
    private boolean puedeReabrir;
    private boolean puedeSolicitarCuenta;
    private EstadoPedidoResponse estadoPedido;
    private List<DetallePedidoMesaResponse> detalles;

    public static PedidoMesaResponse from(Pedido pedido) {
        if (pedido == null) {
            return null;
        }

        PedidoResponse completo = PedidoResponse.from(pedido);
        return new PedidoMesaResponse(
                pedido.getIdPedido(),
                pedido.getTotal(),
                completo.getEtapaFlujo(),
                completo.isEditable(),
                completo.isPuedeEnviarCocina(),
                completo.isPuedeReabrir(),
                completo.isPuedeSolicitarCuenta(),
                EstadoPedidoResponse.from(pedido.getEstadoPedido()),
                (pedido.getDetalles() == null ? Collections.<DetallePedido>emptyList() : pedido.getDetalles())
                        .stream()
                        .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                        .map(DetallePedidoMesaResponse::from)
                        .toList()
        );
    }
}
