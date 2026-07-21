package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ReporteVentasResponse;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int MAXIMO_DIAS = 366;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public ReporteVentasResponse obtenerVentas(LocalDate desde, LocalDate hasta) {
        List<Pedido> ventas = consultarVentas(desde, hasta);
        BigDecimal total = ventas.stream().map(Pedido::getTotal).map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> metodos = new LinkedHashMap<>();
        Map<Integer, BigDecimal> ventasPorHora = new LinkedHashMap<>();
        for (int hora = 0; hora < 24; hora++) ventasPorHora.put(hora, BigDecimal.ZERO);
        for (Pedido pedido : ventas) {
            metodos.merge(normalizarMetodoPago(pedido.getMetodoPago()), 1L, Long::sum);
            ventasPorHora.merge(fechaVenta(pedido).getHour(), valor(pedido.getTotal()), BigDecimal::add);
        }
        long totalTickets = ventas.size();
        List<ReporteVentasResponse.MetodoPagoResumen> resumenMetodos = metodos.entrySet().stream()
                .map(entry -> new ReporteVentasResponse.MetodoPagoResumen(entry.getKey(), entry.getValue(),
                        totalTickets == 0 ? BigDecimal.ZERO.setScale(1)
                                : BigDecimal.valueOf(entry.getValue()).multiply(CIEN)
                                        .divide(BigDecimal.valueOf(totalTickets), 1, RoundingMode.HALF_UP))).toList();
        List<ReporteVentasResponse.VentasPorHora> serieHoras = ventasPorHora.entrySet().stream()
                .map(entry -> new ReporteVentasResponse.VentasPorHora(entry.getKey(), moneda(entry.getValue()))).toList();
        List<ReporteVentasResponse.VentaReciente> ultimasVentas = ventas.stream()
                .sorted(Comparator.comparing(this::fechaVenta).reversed()).limit(10)
                .map(pedido -> new ReporteVentasResponse.VentaReciente(
                        pedido.getIdPedido(), "T-%04d".formatted(pedido.getIdPedido()), fechaVenta(pedido),
                        pedido.getIdMesa() == null ? null : pedido.getIdMesa().getNumeroMesa(),
                        pedido.getClienteNombre() == null || pedido.getClienteNombre().isBlank()
                                ? "Publico general" : pedido.getClienteNombre(),
                        normalizarMetodoPago(pedido.getMetodoPago()), moneda(pedido.getTotal()))).toList();
        return new ReporteVentasResponse(desde, hasta, moneda(total), totalTickets,
                totalTickets == 0 ? moneda(BigDecimal.ZERO)
                        : moneda(total.divide(BigDecimal.valueOf(totalTickets), 4, RoundingMode.HALF_UP)),
                resumenMetodos, serieHoras, ultimasVentas);
    }

    private List<Pedido> consultarVentas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return pedidoRepository.findVentasParaReporte(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        Preconditions.checkArgument(desde != null && hasta != null, "Debe indicar las fechas desde y hasta.");
        Preconditions.checkArgument(!desde.isAfter(hasta), "La fecha desde no puede ser posterior a la fecha hasta.");
        Preconditions.checkArgument(ChronoUnit.DAYS.between(desde, hasta) <= MAXIMO_DIAS,
                "El periodo maximo permitido es de 366 dias.");
    }

    private LocalDateTime fechaVenta(Pedido pedido) {
        return pedido.getFechaPago() == null ? pedido.getFechaPedido() : pedido.getFechaPago();
    }

    private String normalizarMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) return "No registrado";
        return switch (metodoPago.trim().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "EFECTIVO" -> "Efectivo";
            case "TARJETA" -> "Tarjeta";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            case "TRANSFERENCIA" -> "Transferencia";
            default -> metodoPago.trim();
        };
    }

    private BigDecimal valor(BigDecimal dato) { return dato == null ? BigDecimal.ZERO : dato; }
    private BigDecimal moneda(BigDecimal dato) { return valor(dato).setScale(2, RoundingMode.HALF_UP); }
}
