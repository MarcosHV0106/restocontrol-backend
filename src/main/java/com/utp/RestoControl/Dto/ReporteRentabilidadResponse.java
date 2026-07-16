package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteRentabilidadResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal ingresosTotales,
        BigDecimal costoInsumosTeorico,
        BigDecimal margenNetoPorcentaje,
        BigDecimal margenNetoMonto,
        boolean costosCompletos,
        long platosSinCosto,
        List<SerieDiaria> ventasVsCostosPorDia,
        List<PlatoRentable> platosMasRentables
) {
    public record SerieDiaria(LocalDate fecha, BigDecimal ventas, BigDecimal costos) {}

    public record PlatoRentable(
            Integer idAlimento,
            String plato,
            long vendidos,
            BigDecimal precioVentaPromedio,
            BigDecimal costoReceta,
            BigDecimal margenPorcentaje
    ) {}
}
