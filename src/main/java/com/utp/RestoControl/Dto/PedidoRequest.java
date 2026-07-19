
package com.utp.RestoControl.Dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequest {

    private Integer idMesa;

    private Integer idUsuario;

    private Integer idModalidadPedido;

    private Integer idEstadoPedido;

    private String observacion;

    private String clienteNombre;

    private String clienteTelefono;

    private String direccionEntrega;

    private List<DetallePedidoRequest> detalles;

    public PedidoRequest(
            Integer idMesa,
            Integer idUsuario,
            Integer idModalidadPedido,
            Integer idEstadoPedido,
            String observacion,
            List<DetallePedidoRequest> detalles
    ) {
        this(idMesa, idUsuario, idModalidadPedido, idEstadoPedido, observacion, null, null, null, detalles);
    }

}
