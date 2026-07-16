package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.PagoCobro;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PagoCobroResponse {

    private Integer idPagoCobro;
    private Integer secuencia;
    private String metodoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private String tipoComprobante;
    private String numeroComprobante;
    private String documentoCliente;
    private String razonSocial;
    private String referencia;

    public static PagoCobroResponse from(PagoCobro pago) {
        return new PagoCobroResponse(
                pago.getIdPagoCobro(),
                pago.getSecuencia(),
                pago.getMetodoPago(),
                pago.getMonto(),
                pago.getMontoRecibido(),
                pago.getVuelto(),
                pago.getTipoComprobante(),
                pago.getNumeroComprobante(),
                pago.getDocumentoCliente(),
                pago.getRazonSocial(),
                pago.getReferencia()
        );
    }
}
