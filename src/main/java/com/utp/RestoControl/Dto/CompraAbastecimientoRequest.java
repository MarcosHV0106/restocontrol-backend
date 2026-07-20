package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraAbastecimientoRequest(
        Integer idProveedor,
        LocalDate fechaCompra,
        String numeroDocumento,
        String observacion,
        List<Detalle> detalles
) {
    public record Detalle(
            Integer idInsumo,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDate fechaVencimiento
    ) {
    }
}
