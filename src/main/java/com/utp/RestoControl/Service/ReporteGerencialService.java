package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ReporteGerencialResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReporteGerencialService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int MAXIMO_DIAS = 366;

    private final PedidoRepository pedidoRepository;
    private final AlimentoRepository alimentoRepository;

    @Transactional(readOnly = true)
    public ReporteGerencialResponse obtener(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        List<Pedido> ventas = pedidoRepository.findVentasParaReporte(inicio, finExclusivo);
        List<Pedido> cancelados = pedidoRepository.findCanceladosParaReporte(inicio, finExclusivo);
        List<Alimento> alimentos = alimentoRepository.findByEliminadoFalse();

        BigDecimal ventasTotales = ventas.stream()
                .map(Pedido::getTotal)
                .map(this::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ReporteGerencialResponse.ProductoVendido> productos = resumirProductos(alimentos, ventas);

        ReporteGerencialResponse.IndicadoresDecision indicadores =
                new ReporteGerencialResponse.IndicadoresDecision(
                        moneda(ventasTotales),
                        ventas.size(),
                        cancelados.size(),
                        porcentaje(BigDecimal.valueOf(cancelados.size()),
                                BigDecimal.valueOf(ventas.size() + cancelados.size())),
                        ventas.isEmpty() ? moneda(BigDecimal.ZERO)
                                : moneda(ventasTotales.divide(BigDecimal.valueOf(ventas.size()), 4, RoundingMode.HALF_UP))
                );

        return new ReporteGerencialResponse(
                desde,
                hasta,
                indicadores,
                agruparVentas(ventas, Agrupacion.DIA),
                agruparVentas(ventas, Agrupacion.SEMANA),
                agruparVentas(ventas, Agrupacion.MES),
                productos.stream().filter(producto -> producto.cantidadVendida() > 0)
                        .sorted(Comparator.comparingLong(ReporteGerencialResponse.ProductoVendido::cantidadVendida)
                                .reversed().thenComparing(ReporteGerencialResponse.ProductoVendido::producto))
                        .limit(10).toList(),
                productos.stream()
                        .sorted(Comparator.comparingLong(ReporteGerencialResponse.ProductoVendido::cantidadVendida)
                                .thenComparing(ReporteGerencialResponse.ProductoVendido::producto))
                        .limit(10).toList(),
                resumirCancelados(cancelados),
                resumirModalidades(ventas, ventasTotales)
        );
    }

    private List<ReporteGerencialResponse.VentaPeriodo> agruparVentas(List<Pedido> ventas, Agrupacion agrupacion) {
        Map<LocalDate, AcumuladoVenta> acumulados = new TreeMap<>();
        for (Pedido pedido : ventas) {
            LocalDate fecha = fechaVenta(pedido).toLocalDate();
            LocalDate clave = switch (agrupacion) {
                case DIA -> fecha;
                case SEMANA -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case MES -> fecha.withDayOfMonth(1);
            };
            AcumuladoVenta acumulado = acumulados.computeIfAbsent(clave, ignorado -> new AcumuladoVenta());
            acumulado.pedidos++;
            acumulado.ventas = acumulado.ventas.add(valor(pedido.getTotal()));
        }
        return acumulados.entrySet().stream().map(entry -> new ReporteGerencialResponse.VentaPeriodo(
                etiquetaPeriodo(entry.getKey(), agrupacion), entry.getKey(),
                finPeriodo(entry.getKey(), agrupacion), entry.getValue().pedidos,
                moneda(entry.getValue().ventas))).toList();
    }

    private List<ReporteGerencialResponse.ProductoVendido> resumirProductos(
            List<Alimento> alimentos, List<Pedido> ventas) {
        Map<Integer, AcumuladoProducto> acumulados = new LinkedHashMap<>();
        for (Alimento alimento : alimentos) {
            acumulados.put(alimento.getIdAlimento(), new AcumuladoProducto(
                    alimento.getIdAlimento(), alimento.getNombreAlimento(),
                    alimento.getCategoria() == null ? "Sin categoria" : alimento.getCategoria().getNombreCategoria()));
        }
        for (Pedido pedido : ventas) {
            for (DetallePedido detalle : detallesActivos(pedido)) {
                Alimento alimento = detalle.getIdAlimento();
                AcumuladoProducto acumulado = acumulados.computeIfAbsent(alimento.getIdAlimento(), id ->
                        new AcumuladoProducto(id, alimento.getNombreAlimento(),
                                alimento.getCategoria() == null ? "Sin categoria" : alimento.getCategoria().getNombreCategoria()));
                acumulado.cantidad += detalle.getCantidad();
                acumulado.ingresos = acumulado.ingresos.add(valor(detalle.getSubtotal()));
            }
        }
        return acumulados.values().stream().map(dato -> new ReporteGerencialResponse.ProductoVendido(
                dato.idAlimento, dato.producto, dato.categoria, dato.cantidad, moneda(dato.ingresos))).toList();
    }

    private List<ReporteGerencialResponse.PedidoCancelado> resumirCancelados(List<Pedido> pedidos) {
        return pedidos.stream().sorted(Comparator.comparing(this::fechaCancelacion).reversed())
                .map(pedido -> new ReporteGerencialResponse.PedidoCancelado(
                        pedido.getIdPedido(), fechaCancelacion(pedido),
                        pedido.getModalidadPedido() == null ? "No registrada" : pedido.getModalidadPedido().getNombreModalidad(),
                        pedido.getIdMesa() == null ? null : pedido.getIdMesa().getNumeroMesa(),
                        nombreUsuario(pedido.getUsuario()), textoO(pedido.getMotivoCancelacion(), "Sin motivo registrado"),
                        moneda(pedido.getTotal()))).toList();
    }

    private List<ReporteGerencialResponse.VentasModalidad> resumirModalidades(
            List<Pedido> ventas, BigDecimal ventasTotales) {
        Map<String, AcumuladoVenta> acumulados = new LinkedHashMap<>();
        for (Pedido pedido : ventas) {
            String modalidad = pedido.getModalidadPedido() == null
                    ? "No registrada" : pedido.getModalidadPedido().getNombreModalidad();
            AcumuladoVenta acumulado = acumulados.computeIfAbsent(modalidad, ignorado -> new AcumuladoVenta());
            acumulado.pedidos++;
            acumulado.ventas = acumulado.ventas.add(valor(pedido.getTotal()));
        }
        return acumulados.entrySet().stream()
                .sorted(Map.Entry.<String, AcumuladoVenta>comparingByValue(
                        Comparator.comparing(AcumuladoVenta::ventas)).reversed())
                .map(entry -> new ReporteGerencialResponse.VentasModalidad(
                        entry.getKey(), entry.getValue().pedidos, moneda(entry.getValue().ventas),
                        porcentaje(entry.getValue().ventas, ventasTotales))).toList();
    }

    private List<DetallePedido> detallesActivos(Pedido pedido) {
        if (pedido.getDetalles() == null) return List.of();
        return pedido.getDetalles().stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .filter(detalle -> detalle.getCantidad() != null && detalle.getCantidad() > 0)
                .filter(detalle -> detalle.getIdAlimento() != null).toList();
    }

    private String etiquetaPeriodo(LocalDate inicio, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> inicio.toString();
            case SEMANA -> "%d-S%02d".formatted(inicio.get(WeekFields.ISO.weekBasedYear()),
                    inicio.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case MES -> YearMonth.from(inicio).toString();
        };
    }

    private LocalDate finPeriodo(LocalDate inicio, Agrupacion agrupacion) {
        return switch (agrupacion) {
            case DIA -> inicio;
            case SEMANA -> inicio.plusDays(6);
            case MES -> inicio.with(TemporalAdjusters.lastDayOfMonth());
        };
    }

    private LocalDateTime fechaVenta(Pedido pedido) {
        return pedido.getFechaPago() == null ? pedido.getFechaPedido() : pedido.getFechaPago();
    }

    private LocalDateTime fechaCancelacion(Pedido pedido) {
        return pedido.getFechaCancelacion() == null ? pedido.getFechaPedido() : pedido.getFechaCancelacion();
    }

    private String nombreUsuario(Usuario usuario) {
        if (usuario == null) return "Sistema";
        String nombre = String.join(" ", textoO(usuario.getNombre(), ""), textoO(usuario.getApellido(), "")).trim();
        return nombre.isBlank() ? textoO(usuario.getCorreo(), "Sistema") : nombre;
    }

    private String textoO(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor.trim();
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        Preconditions.checkArgument(desde != null && hasta != null, "Debe indicar las fechas desde y hasta.");
        Preconditions.checkArgument(!desde.isAfter(hasta), "La fecha desde no puede ser posterior a la fecha hasta.");
        Preconditions.checkArgument(ChronoUnit.DAYS.between(desde, hasta) <= MAXIMO_DIAS,
                "El periodo maximo permitido es de 366 dias.");
    }

    private BigDecimal valor(BigDecimal dato) { return dato == null ? BigDecimal.ZERO : dato; }
    private BigDecimal moneda(BigDecimal dato) { return valor(dato).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal porcentaje(BigDecimal dato, BigDecimal base) {
        if (valor(base).compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        return valor(dato).multiply(CIEN).divide(base, 1, RoundingMode.HALF_UP);
    }

    private enum Agrupacion { DIA, SEMANA, MES }

    private static final class AcumuladoVenta {
        private long pedidos;
        private BigDecimal ventas = BigDecimal.ZERO;
        private BigDecimal ventas() { return ventas; }
    }

    private static final class AcumuladoProducto {
        private final Integer idAlimento;
        private final String producto;
        private final String categoria;
        private long cantidad;
        private BigDecimal ingresos = BigDecimal.ZERO;
        private AcumuladoProducto(Integer idAlimento, String producto, String categoria) {
            this.idAlimento = idAlimento;
            this.producto = producto;
            this.categoria = categoria;
        }
    }
}
