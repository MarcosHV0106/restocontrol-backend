package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReporteVentasResponse(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal ventasBrutasTotales,
        long totalTickets,
        BigDecimal ticketPromedio,
        List<MetodoPagoResumen> metodosPago,
        List<VentasPorHora> ventasPorHora,
        List<VentaReciente> ultimasVentas
) {
    public record MetodoPagoResumen(String metodo, long cantidad, BigDecimal porcentaje) {}

    public record VentasPorHora(int hora, BigDecimal ventas) {}

    public record VentaReciente(
            Integer idPedido,
            String ticket,
            LocalDateTime fechaHora,
            Integer numeroMesa,
            String cliente,
            String metodoPago,
            BigDecimal total
    ) {}
}
