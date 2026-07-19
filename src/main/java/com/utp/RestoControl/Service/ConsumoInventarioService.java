package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import com.utp.RestoControl.Repository.RecetaAlimentoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsumoInventarioService {

    public static final String TIPO_CONSUMO_PEDIDO = "CONSUMO_PEDIDO";
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final RecetaAlimentoRepository recetaRepository;
    private final LoteInsumoRepository loteRepository;
    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaInventarioService alertaService;

    @Transactional
    public void consumirParaPedido(Pedido pedido) {
        Preconditions.checkArgument(pedido != null && pedido.getIdPedido() != null,
                "El pedido es obligatorio para descontar inventario.");
        if (pedido.getFechaConsumoInventario() != null) {
            return;
        }

        List<DetallePedido> detalles = detallesActivos(pedido);
        if (detalles.isEmpty()) {
            throw new ConflictException("El pedido no contiene platos para preparar.");
        }

        Map<Integer, RequerimientoInsumo> requerimientos = construirRequerimientos(detalles);
        PlanificacionConsumo planificacion = planificarConsumo(requerimientos);
        List<PlanConsumo> plan = planificacion.consumos();
        Usuario responsable = obtenerUsuarioActual();
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        List<MovimientoInventario> movimientos = new ArrayList<>();

        for (PlanConsumo consumo : plan) {
            LoteInsumo lote = consumo.lote();
            lote.setCantidadActual(lote.getCantidadActual().subtract(consumo.cantidad()));
            if (lote.getCantidadActual().compareTo(BigDecimal.ZERO) == 0) {
                lote.setEstado("AGOTADO");
            }
            movimientos.add(nuevoMovimiento(pedido, lote, consumo.cantidad(), responsable, ahora));
        }

        loteRepository.saveAll(plan.stream().map(PlanConsumo::lote).distinct().toList());
        movimientoRepository.saveAll(movimientos);
        actualizarStocks(requerimientos, planificacion.stockRestante());
        pedido.setFechaConsumoInventario(ahora);
        alertaService.sincronizar();
    }

    private List<DetallePedido> detallesActivos(Pedido pedido) {
        return pedido.getDetalles() == null ? List.of() : pedido.getDetalles().stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .filter(detalle -> detalle.getCantidad() != null && detalle.getCantidad() > 0)
                .toList();
    }

    private Map<Integer, RequerimientoInsumo> construirRequerimientos(List<DetallePedido> detalles) {
        Map<Integer, RequerimientoInsumo> requerimientos = new LinkedHashMap<>();
        List<String> platosSinReceta = new ArrayList<>();

        for (DetallePedido detalle : detalles) {
            List<RecetaAlimento> receta = recetaRepository
                    .findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(
                            detalle.getIdAlimento().getIdAlimento()
                    );
            if (receta.isEmpty()) {
                platosSinReceta.add(detalle.getIdAlimento().getNombreAlimento());
                continue;
            }
            for (RecetaAlimento ingrediente : receta) {
                if (ingrediente.getCantidad() == null
                        || ingrediente.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ConflictException("La receta de "
                            + detalle.getIdAlimento().getNombreAlimento()
                            + " contiene una cantidad de insumo inválida.");
                }
                Insumo insumo = ingrediente.getInsumo();
                BigDecimal cantidad = ingrediente.getCantidad()
                        .multiply(BigDecimal.valueOf(detalle.getCantidad()));
                RequerimientoInsumo acumulado = requerimientos.computeIfAbsent(
                        insumo.getIdInsumo(), id -> new RequerimientoInsumo(insumo)
                );
                acumulado.cantidad = acumulado.cantidad.add(cantidad);
            }
        }

        if (!platosSinReceta.isEmpty()) {
            throw new ConflictException("Configura la receta antes de iniciar: "
                    + String.join(", ", platosSinReceta.stream().distinct().toList()) + ".");
        }
        return requerimientos;
    }

    private PlanificacionConsumo planificarConsumo(
            Map<Integer, RequerimientoInsumo> requerimientos
    ) {
        List<PlanConsumo> plan = new ArrayList<>();
        List<String> faltantes = new ArrayList<>();
        Map<Integer, BigDecimal> stockRestante = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now(ZONA_LIMA);

        requerimientos.values().stream()
                .sorted(Comparator.comparing(requerimiento -> requerimiento.insumo.getIdInsumo()))
                .forEach(requerimiento -> {
                    BigDecimal pendiente = requerimiento.cantidad;
                    List<LoteInsumo> lotes = loteRepository.findConsumiblesParaPedido(
                            requerimiento.insumo.getIdInsumo(), hoy
                    );
                    BigDecimal disponible = lotes.stream()
                            .map(LoteInsumo::getCantidadActual)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    for (LoteInsumo lote : lotes) {
                        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
                            break;
                        }
                        BigDecimal cantidad = lote.getCantidadActual().min(pendiente);
                        plan.add(new PlanConsumo(lote, cantidad));
                        pendiente = pendiente.subtract(cantidad);
                    }
                    if (pendiente.compareTo(BigDecimal.ZERO) > 0) {
                        faltantes.add("%s %s de %s".formatted(
                                pendiente.stripTrailingZeros().toPlainString(),
                                requerimiento.insumo.getUnidadMedida(),
                                requerimiento.insumo.getNombreInsumo()
                        ));
                    } else {
                        stockRestante.put(
                                requerimiento.insumo.getIdInsumo(),
                                disponible.subtract(requerimiento.cantidad)
                        );
                    }
                });

        if (!faltantes.isEmpty()) {
            throw new ConflictException("Inventario insuficiente. Faltan "
                    + String.join("; ", faltantes) + ". Repón los lotes antes de iniciar.");
        }
        return new PlanificacionConsumo(plan, stockRestante);
    }

    private MovimientoInventario nuevoMovimiento(
            Pedido pedido,
            LoteInsumo lote,
            BigDecimal cantidad,
            Usuario responsable,
            LocalDateTime fecha
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(TIPO_CONSUMO_PEDIDO);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo("Consumo automático de receta del pedido #" + pedido.getIdPedido());
        movimiento.setFechaMovimiento(fecha);
        movimiento.setReferencia("PEDIDO-" + pedido.getIdPedido());
        movimiento.setInsumo(lote.getInsumo());
        movimiento.setLote(lote);
        movimiento.setUsuario(responsable);
        movimiento.setPedido(pedido);
        movimiento.setEliminado(false);
        return movimiento;
    }

    private void actualizarStocks(
            Map<Integer, RequerimientoInsumo> requerimientos,
            Map<Integer, BigDecimal> stockRestante
    ) {
        for (RequerimientoInsumo requerimiento : requerimientos.values()) {
            requerimiento.insumo.setStockActual(
                    stockRestante.getOrDefault(requerimiento.insumo.getIdInsumo(), BigDecimal.ZERO)
            );
            insumoRepository.save(requerimiento.insumo);
        }
    }

    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
        }
        throw new IllegalStateException("No se pudo identificar al responsable del consumo de inventario.");
    }

    private static final class RequerimientoInsumo {
        private final Insumo insumo;
        private BigDecimal cantidad = BigDecimal.ZERO;

        private RequerimientoInsumo(Insumo insumo) {
            this.insumo = insumo;
        }
    }

    private record PlanConsumo(LoteInsumo lote, BigDecimal cantidad) {
    }

    private record PlanificacionConsumo(
            List<PlanConsumo> consumos,
            Map<Integer, BigDecimal> stockRestante
    ) {
    }
}
