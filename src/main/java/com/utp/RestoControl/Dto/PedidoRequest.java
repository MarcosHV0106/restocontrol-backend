
package com.utp.RestoControl.Dto;

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

}