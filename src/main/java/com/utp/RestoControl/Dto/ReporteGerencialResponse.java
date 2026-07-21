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
        List<PedidoCancelado> pedidosCancelados,
        List<VentasModalidad> ventasPorModalidad
) {
    public record IndicadoresDecision(
            BigDecimal ventasTotales,
            long pedidosPagados,
            long pedidosCancelados,
            BigDecimal tasaCancelacion,
            BigDecimal ticketPromedio
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
}
