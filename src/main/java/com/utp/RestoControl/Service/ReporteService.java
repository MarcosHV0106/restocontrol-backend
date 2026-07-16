package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ReporteRentabilidadResponse;
import com.utp.RestoControl.Dto.ReporteVentasResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    public ReporteRentabilidadResponse obtenerRentabilidad(LocalDate desde, LocalDate hasta) {
        List<Pedido> ventas = consultarVentas(desde, hasta);
        Map<LocalDate, AcumuladoDia> porDia = inicializarDias(desde, hasta);
        Map<Integer, AcumuladoPlato> porPlato = new LinkedHashMap<>();
        Set<Integer> platosConCostoIncompleto = new LinkedHashSet<>();
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal costos = BigDecimal.ZERO;

        for (Pedido pedido : ventas) {
            LocalDate fecha = fechaVenta(pedido).toLocalDate();
            BigDecimal totalPedido = valor(pedido.getTotal());
            ingresos = ingresos.add(totalPedido);
            porDia.get(fecha).ventas = porDia.get(fecha).ventas.add(totalPedido);

            for (DetallePedido detalle : detallesActivos(pedido)) {
                Alimento alimento = detalle.getIdAlimento();
                BigDecimal costoUnitario = calcularCostoReceta(alimento);
                BigDecimal costoDetalle = costoUnitario.multiply(BigDecimal.valueOf(detalle.getCantidad()));
                BigDecimal ventaDetalle = valor(detalle.getSubtotal());
                costos = costos.add(costoDetalle);
                porDia.get(fecha).costos = porDia.get(fecha).costos.add(costoDetalle);

                AcumuladoPlato acumulado = porPlato.computeIfAbsent(
                        alimento.getIdAlimento(),
                        id -> new AcumuladoPlato(alimento.getNombreAlimento())
                );
                acumulado.vendidos += detalle.getCantidad();
                acumulado.ventas = acumulado.ventas.add(ventaDetalle);
                acumulado.costos = acumulado.costos.add(costoDetalle);

                if (!tieneCostoCompleto(alimento)) {
                    platosConCostoIncompleto.add(alimento.getIdAlimento());
                }
            }
        }

        BigDecimal margenMonto = ingresos.subtract(costos);
        BigDecimal margenPorcentaje = porcentaje(margenMonto, ingresos);
        List<ReporteRentabilidadResponse.SerieDiaria> serie = porDia.entrySet().stream()
                .map(entry -> new ReporteRentabilidadResponse.SerieDiaria(
                        entry.getKey(),
                        moneda(entry.getValue().ventas),
                        moneda(entry.getValue().costos)))
                .toList();

        List<ReporteRentabilidadResponse.PlatoRentable> platos = porPlato.entrySet().stream()
                .sorted(Map.Entry.<Integer, AcumuladoPlato>comparingByValue(
                        Comparator.comparing(AcumuladoPlato::margenTotal)).reversed())
                .limit(5)
                .map(entry -> {
                    AcumuladoPlato dato = entry.getValue();
                    BigDecimal vendidos = BigDecimal.valueOf(dato.vendidos);
                    return new ReporteRentabilidadResponse.PlatoRentable(
                            entry.getKey(),
                            dato.nombre,
                            dato.vendidos,
                            dividir(dato.ventas, vendidos),
                            dividir(dato.costos, vendidos),
                            porcentaje(dato.margenTotal(), dato.ventas)
                    );
                })
                .toList();

        return new ReporteRentabilidadResponse(
                desde,
                hasta,
                moneda(ingresos),
                moneda(costos),
                margenPorcentaje,
                moneda(margenMonto),
                platosConCostoIncompleto.isEmpty(),
                platosConCostoIncompleto.size(),
                serie,
                platos
        );
    }

    @Transactional(readOnly = true)
    public ReporteVentasResponse obtenerVentas(LocalDate desde, LocalDate hasta) {
        List<Pedido> ventas = consultarVentas(desde, hasta);
        BigDecimal total = ventas.stream()
                .map(Pedido::getTotal)
                .map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> metodos = new LinkedHashMap<>();
        Map<Integer, BigDecimal> ventasPorHora = new LinkedHashMap<>();
        for (int hora = 0; hora < 24; hora++) {
            ventasPorHora.put(hora, BigDecimal.ZERO);
        }

        for (Pedido pedido : ventas) {
            metodos.merge(normalizarMetodoPago(pedido.getMetodoPago()), 1L, Long::sum);
            int hora = fechaVenta(pedido).getHour();
            ventasPorHora.merge(hora, valor(pedido.getTotal()), BigDecimal::add);
        }

        long totalTickets = ventas.size();
        List<ReporteVentasResponse.MetodoPagoResumen> resumenMetodos = metodos.entrySet().stream()
                .map(entry -> new ReporteVentasResponse.MetodoPagoResumen(
                        entry.getKey(),
                        entry.getValue(),
                        totalTickets == 0
                                ? BigDecimal.ZERO.setScale(1)
                                : BigDecimal.valueOf(entry.getValue())
                                        .multiply(CIEN)
                                        .divide(BigDecimal.valueOf(totalTickets), 1, RoundingMode.HALF_UP)))
                .toList();

        List<ReporteVentasResponse.VentasPorHora> serieHoras = ventasPorHora.entrySet().stream()
                .map(entry -> new ReporteVentasResponse.VentasPorHora(
                        entry.getKey(), moneda(entry.getValue())))
                .toList();

        List<ReporteVentasResponse.VentaReciente> ultimasVentas = ventas.stream()
                .sorted(Comparator.comparing(this::fechaVenta).reversed())
                .limit(10)
                .map(pedido -> new ReporteVentasResponse.VentaReciente(
                        pedido.getIdPedido(),
                        "T-%04d".formatted(pedido.getIdPedido()),
                        fechaVenta(pedido),
                        pedido.getIdMesa().getNumeroMesa(),
                        "Público general",
                        normalizarMetodoPago(pedido.getMetodoPago()),
                        moneda(valor(pedido.getTotal()))))
                .toList();

        return new ReporteVentasResponse(
                desde,
                hasta,
                moneda(total),
                totalTickets,
                totalTickets == 0 ? moneda(BigDecimal.ZERO) : dividir(total, BigDecimal.valueOf(totalTickets)),
                resumenMetodos,
                serieHoras,
                ultimasVentas
        );
    }

    private List<Pedido> consultarVentas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        return pedidoRepository.findVentasParaReporte(inicio, finExclusivo);
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        Preconditions.checkArgument(desde != null && hasta != null,
                "Debe indicar las fechas desde y hasta.");
        Preconditions.checkArgument(!desde.isAfter(hasta),
                "La fecha desde no puede ser posterior a la fecha hasta.");
        Preconditions.checkArgument(ChronoUnit.DAYS.between(desde, hasta) <= MAXIMO_DIAS,
                "El periodo máximo permitido es de 366 días.");
    }

    private Map<LocalDate, AcumuladoDia> inicializarDias(LocalDate desde, LocalDate hasta) {
        Map<LocalDate, AcumuladoDia> dias = new LinkedHashMap<>();
        desde.datesUntil(hasta.plusDays(1)).forEach(fecha -> dias.put(fecha, new AcumuladoDia()));
        return dias;
    }

    private List<DetallePedido> detallesActivos(Pedido pedido) {
        if (pedido.getDetalles() == null) {
            return List.of();
        }
        return pedido.getDetalles().stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .filter(detalle -> detalle.getCantidad() != null && detalle.getCantidad() > 0)
                .toList();
    }

    private BigDecimal calcularCostoReceta(Alimento alimento) {
        if (alimento.getReceta() == null) {
            return BigDecimal.ZERO;
        }
        return alimento.getReceta().stream()
                .filter(receta -> receta.getCantidad() != null && receta.getInsumo() != null)
                .map(receta -> receta.getCantidad().multiply(valor(receta.getInsumo().getCostoUnitario())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean tieneCostoCompleto(Alimento alimento) {
        List<RecetaAlimento> receta = alimento.getReceta();
        return receta != null
                && !receta.isEmpty()
                && receta.stream().allMatch(detalle -> detalle.getCantidad() != null
                        && detalle.getCantidad().compareTo(BigDecimal.ZERO) > 0
                        && detalle.getInsumo() != null
                        && detalle.getInsumo().getCostoUnitario() != null
                        && detalle.getInsumo().getCostoUnitario().compareTo(BigDecimal.ZERO) > 0);
    }

    private LocalDateTime fechaVenta(Pedido pedido) {
        return pedido.getFechaPago() == null ? pedido.getFechaPedido() : pedido.getFechaPago();
    }

    private String normalizarMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            return "No registrado";
        }
        return switch (metodoPago.trim().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "EFECTIVO" -> "Efectivo";
            case "TARJETA" -> "Tarjeta";
            case "YAPE" -> "Yape";
            case "PLIN" -> "Plin";
            case "TRANSFERENCIA" -> "Transferencia";
            default -> metodoPago.trim();
        };
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal moneda(BigDecimal valor) {
        return valor(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return moneda(BigDecimal.ZERO);
        }
        return dividendo.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentaje(BigDecimal monto, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return monto.multiply(CIEN).divide(base, 1, RoundingMode.HALF_UP);
    }

    private static final class AcumuladoDia {
        private BigDecimal ventas = BigDecimal.ZERO;
        private BigDecimal costos = BigDecimal.ZERO;
    }

    private static final class AcumuladoPlato {
        private final String nombre;
        private long vendidos;
        private BigDecimal ventas = BigDecimal.ZERO;
        private BigDecimal costos = BigDecimal.ZERO;

        private AcumuladoPlato(String nombre) {
            this.nombre = nombre;
        }

        private BigDecimal margenTotal() {
            return ventas.subtract(costos);
        }
    }
}
