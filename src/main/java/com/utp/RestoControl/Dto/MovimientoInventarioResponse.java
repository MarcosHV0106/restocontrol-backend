package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.MovimientoInventario;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Integer idMovimiento,
        String tipoMovimiento,
        BigDecimal cantidad,
        String motivo,
        LocalDateTime fechaMovimiento,
        String referencia,
        Integer idInsumo,
        String insumo,
        String unidadMedida,
        Integer idLote,
        String codigoLote,
        Integer idPedido,
        String responsable
) {
    public static MovimientoInventarioResponse from(MovimientoInventario movimiento) {
        String responsable = movimiento.getUsuario() == null
                ? "Sistema"
                : (movimiento.getUsuario().getNombre() + " " + movimiento.getUsuario().getApellido()).trim();
        return new MovimientoInventarioResponse(
                movimiento.getIdMovimiento(),
                movimiento.getTipoMovimiento(),
                movimiento.getCantidad(),
                movimiento.getMotivo(),
                movimiento.getFechaMovimiento(),
                movimiento.getReferencia(),
                movimiento.getInsumo().getIdInsumo(),
                movimiento.getInsumo().getNombreInsumo(),
                movimiento.getInsumo().getUnidadMedida(),
                movimiento.getLote() == null ? null : movimiento.getLote().getIdLote(),
                movimiento.getLote() == null ? null : movimiento.getLote().getCodigo(),
                movimiento.getPedido() == null ? null : movimiento.getPedido().getIdPedido(),
                responsable.isBlank() ? "Sistema" : responsable
        );
    }
}
