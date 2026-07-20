package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReporteGerencialResponse(
        LocalDate desde,
        LocalDate hasta,
        IndicadoresDecision indicadores,
        List<VentaPeriodo> ventasPorDia,
        List<VentaPeriodo> ventasPorSemana,
        List<VentaPeriodo> ventasPorMes,
        List<ProductoVendido> productosMasVendidos,
        List<ProductoVendido> productosMenosVendidos,
        List<ConsumoInsumo> consumoInsumos,
        List<PedidoCancelado> pedidosCancelados,
        List<VentasModalidad> ventasPorModalidad,
        List<MovimientoInventarioItem> movimientosInventario,
        List<InsumoAgotado> insumosAgotados
) {
    public record IndicadoresDecision(
            BigDecimal ventasTotales,
            BigDecimal comprasRegistradas,
            BigDecimal costoConsumoInventario,
            BigDecimal valorInventarioActual,
            long pedidosPagados,
            long pedidosCancelados,
            BigDecimal tasaCancelacion,
            long insumosAgotados,
            long insumosBajoMinimo,
            long alertasActivas
    ) {}

    public record VentaPeriodo(
            String periodo,
            LocalDate inicio,
            LocalDate fin,
            long pedidos,
            BigDecimal ventas
    ) {}

    public record ProductoVendido(
            Integer idAlimento,
            String producto,
            String categoria,
            long cantidadVendida,
            BigDecimal ingresos
    ) {}

    public record ConsumoInsumo(
            Integer idInsumo,
            String insumo,
            String unidadMedida,
            BigDecimal cantidadConsumida,
            BigDecimal costoTeorico,
            long movimientos,
            LocalDateTime ultimoConsumo
    ) {}

    public record PedidoCancelado(
            Integer idPedido,
            LocalDateTime fechaCancelacion,
            String modalidad,
            Integer numeroMesa,
            String responsable,
            String motivo,
            BigDecimal totalAnulado
    ) {}

    public record VentasModalidad(
            String modalidad,
            long pedidos,
            BigDecimal ventas,
            BigDecimal participacion
    ) {}

    public record MovimientoInventarioItem(
            Integer idMovimiento,
            LocalDateTime fecha,
            String tipo,
            String insumo,
            String unidadMedida,
            BigDecimal cantidad,
            String referencia,
            String responsable
    ) {}

    public record InsumoAgotado(
            Integer idInsumo,
            String insumo,
            String unidadMedida,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            BigDecimal costoUnitario
    ) {}
}
