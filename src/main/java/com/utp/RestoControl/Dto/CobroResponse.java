package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Cobro;
import com.utp.RestoControl.Entity.PagoCobro;
import com.utp.RestoControl.Entity.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CobroResponse {

    private Integer idCobro;
    private Integer idPedido;
    private Integer numeroMesa;
    private LocalDateTime fechaCobro;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal descuento;
    private BigDecimal totalCobrado;
    private BigDecimal totalRecibido;
    private BigDecimal vuelto;
    private Integer idUsuarioCajero;
    private String nombreCajero;
    private List<PagoCobroResponse> pagos;

    public static CobroResponse from(Cobro cobro) {
        Usuario cajero = cobro.getUsuarioCajero();
        List<PagoCobroResponse> pagos = (cobro.getPagos() == null
                ? Collections.<PagoCobro>emptyList()
                : cobro.getPagos())
                .stream()
                .filter(pago -> !Boolean.TRUE.equals(pago.getEliminado()))
                .sorted(Comparator.comparing(PagoCobro::getSecuencia))
                .map(PagoCobroResponse::from)
                .toList();

        return new CobroResponse(
                cobro.getIdCobro(),
                cobro.getPedido().getIdPedido(),
                cobro.getPedido().getIdMesa().getNumeroMesa(),
                cobro.getFechaCobro(),
                cobro.getSubtotal(),
                cobro.getIgv(),
                cobro.getDescuento(),
                cobro.getTotalCobrado(),
                cobro.getTotalRecibido(),
                cobro.getVuelto(),
                cajero.getIdUsuario(),
                nombreCompleto(cajero),
                pagos
        );
    }

    private static String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).trim();
    }
}
