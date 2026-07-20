package com.utp.RestoControl.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ReporteGerencialResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Entity.CompraAbastecimiento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.AlertaInventarioRepository;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.CompraAbastecimientoRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporteGerencialServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private AlimentoRepository alimentoRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private CompraAbastecimientoRepository compraRepository;
    @Mock
    private AlertaInventarioRepository alertaRepository;

    private ReporteGerencialService service;

    @BeforeEach
    void setUp() {
        service = new ReporteGerencialService(
                pedidoRepository,
                movimientoRepository,
                alimentoRepository,
                insumoRepository,
                compraRepository,
                alertaRepository
        );
    }

    @Test
    void consolidaIndicadoresVentasInventarioYCancelaciones() {
        LocalDate desde = LocalDate.of(2026, 5, 1);
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        Insumo carne = crearInsumo(1, "Carne", "kg", "0", "2", "10");
        Insumo papa = crearInsumo(2, "Papa", "kg", "3", "5", "2");
        CategoriaAlimento fondos = new CategoriaAlimento(1, "Fondos", "", false);
        Alimento lomo = crearAlimento(1, "Lomo saltado", fondos);
        Alimento ceviche = crearAlimento(2, "Ceviche", fondos);
        Pedido venta = crearVenta(lomo);
        Pedido cancelado = crearCancelado();
        MovimientoInventario consumo = crearMovimiento(
                1, carne, "CONSUMO_PEDIDO", "1.5000", LocalDateTime.of(2026, 5, 15, 13, 0));
        MovimientoInventario entrada = crearMovimiento(
                2, papa, "ENTRADA_COMPRA", "5.0000", LocalDateTime.of(2026, 5, 12, 9, 0));
        CompraAbastecimiento compra = new CompraAbastecimiento();
        compra.setTotal(new BigDecimal("50.00"));

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo = hasta.plusDays(1).atStartOfDay();
        when(pedidoRepository.findVentasParaReporte(inicio, finExclusivo)).thenReturn(List.of(venta));
        when(pedidoRepository.findCanceladosParaReporte(inicio, finExclusivo)).thenReturn(List.of(cancelado));
        when(movimientoRepository.findParaReporte(inicio, finExclusivo))
                .thenReturn(List.of(consumo, entrada));
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(lomo, ceviche));
        when(insumoRepository.findByEliminadoFalse()).thenReturn(List.of(carne, papa));
        when(compraRepository.findParaReporte(desde, hasta)).thenReturn(List.of(compra));
        when(alertaRepository.countByEstadoAndEliminadoFalse("ACTIVA")).thenReturn(2L);

        ReporteGerencialResponse reporte = service.obtener(desde, hasta);

        assertThat(reporte.indicadores().ventasTotales()).isEqualByComparingTo("100.00");
        assertThat(reporte.indicadores().comprasRegistradas()).isEqualByComparingTo("50.00");
        assertThat(reporte.indicadores().costoConsumoInventario()).isEqualByComparingTo("15.00");
        assertThat(reporte.indicadores().valorInventarioActual()).isEqualByComparingTo("6.00");
        assertThat(reporte.indicadores().pedidosPagados()).isEqualTo(1);
        assertThat(reporte.indicadores().pedidosCancelados()).isEqualTo(1);
        assertThat(reporte.indicadores().tasaCancelacion()).isEqualByComparingTo("50.0");
        assertThat(reporte.indicadores().insumosAgotados()).isEqualTo(1);
        assertThat(reporte.indicadores().insumosBajoMinimo()).isEqualTo(1);
        assertThat(reporte.indicadores().alertasActivas()).isEqualTo(2);

        assertThat(reporte.ventasPorDia()).singleElement().satisfies(periodo -> {
            assertThat(periodo.periodo()).isEqualTo("2026-05-15");
            assertThat(periodo.ventas()).isEqualByComparingTo("100.00");
        });
        assertThat(reporte.ventasPorSemana()).hasSize(1);
        assertThat(reporte.ventasPorMes()).hasSize(1);
        assertThat(reporte.productosMasVendidos()).singleElement()
                .satisfies(producto -> assertThat(producto.cantidadVendida()).isEqualTo(3));
        assertThat(reporte.productosMenosVendidos().getFirst().producto()).isEqualTo("Ceviche");
        assertThat(reporte.consumoInsumos()).singleElement().satisfies(item -> {
            assertThat(item.insumo()).isEqualTo("Carne");
            assertThat(item.cantidadConsumida()).isEqualByComparingTo("1.5000");
        });
        assertThat(reporte.pedidosCancelados()).singleElement()
                .satisfies(item -> assertThat(item.motivo()).isEqualTo("Cliente desistio"));
        assertThat(reporte.ventasPorModalidad()).singleElement()
                .satisfies(item -> assertThat(item.modalidad()).isEqualTo("SALON"));
        assertThat(reporte.movimientosInventario()).hasSize(2);
        assertThat(reporte.insumosAgotados()).singleElement()
                .satisfies(item -> assertThat(item.insumo()).isEqualTo("Carne"));
    }

    @Test
    void rechazaPeriodosMayoresAUnAnio() {
        assertThatThrownBy(() -> service.obtener(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 2, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366 dias");
    }

    private Insumo crearInsumo(
            int id,
            String nombre,
            String unidad,
            String stock,
            String minimo,
            String costo
    ) {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(id);
        insumo.setNombreInsumo(nombre);
        insumo.setUnidadMedida(unidad);
        insumo.setStockActual(new BigDecimal(stock));
        insumo.setStockMinimo(new BigDecimal(minimo));
        insumo.setCostoUnitario(new BigDecimal(costo));
        insumo.setEliminado(false);
        return insumo;
    }

    private Alimento crearAlimento(int id, String nombre, CategoriaAlimento categoria) {
        return Alimento.builder()
                .idAlimento(id)
                .nombreAlimento(nombre)
                .categoria(categoria)
                .precio(new BigDecimal("20.00"))
                .disponible(true)
                .eliminado(false)
                .build();
    }

    private Pedido crearVenta(Alimento alimento) {
        ModalidadPedido modalidad = new ModalidadPedido(1, "SALON", false);
        Pedido pedido = Pedido.builder()
                .idPedido(100)
                .fechaPedido(LocalDateTime.of(2026, 5, 15, 12, 0))
                .fechaPago(LocalDateTime.of(2026, 5, 15, 13, 0))
                .modalidadPedido(modalidad)
                .total(new BigDecimal("100.00"))
                .eliminado(false)
                .build();
        DetallePedido detalle = DetallePedido.builder()
                .idDetalle(1)
                .idPedido(pedido)
                .idAlimento(alimento)
                .cantidad(3)
                .precio_unitario(new BigDecimal("20.00"))
                .subtotal(new BigDecimal("60.00"))
                .eliminado(false)
                .build();
        pedido.setDetalles(List.of(detalle));
        return pedido;
    }

    private Pedido crearCancelado() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Torres");
        return Pedido.builder()
                .idPedido(101)
                .fechaPedido(LocalDateTime.of(2026, 5, 16, 18, 0))
                .fechaCancelacion(LocalDateTime.of(2026, 5, 16, 18, 15))
                .motivoCancelacion("Cliente desistio")
                .modalidadPedido(new ModalidadPedido(2, "DELIVERY", false))
                .usuario(usuario)
                .total(new BigDecimal("40.00"))
                .eliminado(false)
                .build();
    }

    private MovimientoInventario crearMovimiento(
            int id,
            Insumo insumo,
            String tipo,
            String cantidad,
            LocalDateTime fecha
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setIdMovimiento(id);
        movimiento.setInsumo(insumo);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setFechaMovimiento(fecha);
        movimiento.setReferencia("REF-" + id);
        movimiento.setEliminado(false);
        return movimiento;
    }
}
