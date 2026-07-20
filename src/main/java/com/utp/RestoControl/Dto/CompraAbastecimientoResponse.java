package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.CompraAbastecimiento;
import com.utp.RestoControl.Entity.CompraAbastecimientoDetalle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompraAbastecimientoResponse(
        Integer idCompra,
        Integer idProveedor,
        String proveedor,
        String rucProveedor,
        LocalDate fechaCompra,
        LocalDateTime fechaRegistro,
        String numeroDocumento,
        String observacion,
        BigDecimal total,
        Integer idUsuarioAlmacenero,
        String responsable,
        List<Detalle> detalles
) {
    public static CompraAbastecimientoResponse from(CompraAbastecimiento compra) {
        String responsable = (compra.getUsuarioAlmacenero().getNombre() + " "
                + compra.getUsuarioAlmacenero().getApellido()).trim();
        return new CompraAbastecimientoResponse(
                compra.getIdCompra(), compra.getProveedor().getIdProveedor(),
                compra.getProveedor().getRazonSocial(), compra.getProveedor().getRuc(),
                compra.getFechaCompra(), compra.getFechaRegistro(), compra.getNumeroDocumento(),
                compra.getObservacion(), compra.getTotal(), compra.getUsuarioAlmacenero().getIdUsuario(),
                responsable, compra.getDetalles().stream().map(Detalle::from).toList()
        );
    }

    public record Detalle(
            Integer idCompraDetalle,
            Integer idInsumo,
            String insumo,
            String unidadMedida,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal subtotal,
            Integer idLote,
            String codigoLote,
            LocalDate fechaVencimiento
    ) {
        static Detalle from(CompraAbastecimientoDetalle detalle) {
            return new Detalle(
                    detalle.getIdCompraDetalle(), detalle.getInsumo().getIdInsumo(),
                    detalle.getInsumo().getNombreInsumo(), detalle.getInsumo().getUnidadMedida(),
                    detalle.getCantidad(), detalle.getCostoUnitario(), detalle.getSubtotal(),
                    detalle.getLote().getIdLote(), detalle.getLote().getCodigo(),
                    detalle.getLote().getFechaVencimiento()
            );
        }
    }
}
