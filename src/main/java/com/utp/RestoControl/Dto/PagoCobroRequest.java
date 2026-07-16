package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoCobroRequest {

    private String metodoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private String tipoComprobante;
    private String documentoCliente;
    private String razonSocial;
    private String referencia;
}
