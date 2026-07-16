package com.utp.RestoControl.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ReporteRentabilidadResponse;
import com.utp.RestoControl.Dto.ReporteVentasResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.RecetaAlimento;
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
class ReporteServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;

    private ReporteService service;

    @BeforeEach
    void setUp() {
        service = new ReporteService(pedidoRepository);
    }

    @Test
    void calculaRentabilidadConElCostoRealDeLaReceta() {
        Pedido venta = crearVenta();
        configurarConsulta(venta);

        ReporteRentabilidadResponse reporte = service.obtenerRentabilidad(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15));

        assertThat(reporte.ingresosTotales()).isEqualByComparingTo("40.00");
        assertThat(reporte.costoInsumosTeorico()).isEqualByComparingTo("10.00");
        assertThat(reporte.margenNetoMonto()).isEqualByComparingTo("30.00");
        assertThat(reporte.margenNetoPorcentaje()).isEqualByComparingTo("75.0");
        assertThat(reporte.costosCompletos()).isTrue();
        assertThat(reporte.platosMasRentables()).singleElement().satisfies(plato -> {
            assertThat(plato.plato()).isEqualTo("Lomo saltado");
            assertThat(plato.vendidos()).isEqualTo(2);
            assertThat(plato.costoReceta()).isEqualByComparingTo("5.00");
        });
    }

    @Test
    void agrupaVentasPorMetodoHoraYTicket() {
        Pedido venta = crearVenta();
        configurarConsulta(venta);

        ReporteVentasResponse reporte = service.obtenerVentas(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 15));

        assertThat(reporte.ventasBrutasTotales()).isEqualByComparingTo("40.00");
        assertThat(reporte.totalTickets()).isEqualTo(1);
        assertThat(reporte.ticketPromedio()).isEqualByComparingTo("40.00");
        assertThat(reporte.metodosPago()).singleElement().satisfies(metodo -> {
            assertThat(metodo.metodo()).isEqualTo("Yape");
            assertThat(metodo.porcentaje()).isEqualByComparingTo("100.0");
        });
        assertThat(reporte.ventasPorHora().get(14).ventas()).isEqualByComparingTo("40.00");
        assertThat(reporte.ultimasVentas()).singleElement().satisfies(ultima -> {
            assertThat(ultima.ticket()).isEqualTo("T-0320");
            assertThat(ultima.numeroMesa()).isEqualTo(7);
        });
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        assertThatThrownBy(() -> service.obtenerVentas(
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha desde");
    }

    private void configurarConsulta(Pedido venta) {
        when(pedidoRepository.findVentasParaReporte(
                LocalDate.of(2026, 5, 1).atStartOfDay(),
                LocalDate.of(2026, 5, 16).atStartOfDay()))
                .thenReturn(List.of(venta));
    }

    private Pedido crearVenta() {
        Insumo carne = new Insumo();
        carne.setIdInsumo(1);
        carne.setNombreInsumo("Carne");
        carne.setUnidadMedida("kg");
        carne.setCostoUnitario(new BigDecimal("10.0000"));

        Alimento alimento = Alimento.builder()
                .idAlimento(4)
                .nombreAlimento("Lomo saltado")
                .precio(new BigDecimal("20.00"))
                .disponible(true)
                .eliminado(false)
                .build();
        RecetaAlimento receta = RecetaAlimento.builder()
                .idRecetaAlimento(8)
                .alimento(alimento)
                .insumo(carne)
                .cantidad(new BigDecimal("0.5000"))
                .build();
        alimento.setReceta(List.of(receta));

        Mesa mesa = new Mesa();
        mesa.setIdMesa(7);
        mesa.setNumeroMesa(7);

        Pedido pedido = Pedido.builder()
                .idPedido(320)
                .fechaPedido(LocalDateTime.of(2026, 5, 15, 14, 0))
                .fechaPago(LocalDateTime.of(2026, 5, 15, 14, 30))
                .metodoPago("YAPE")
                .idMesa(mesa)
                .total(new BigDecimal("40.00"))
                .eliminado(false)
                .build();
        DetallePedido detalle = DetallePedido.builder()
                .idDetalle(1)
                .cantidad(2)
                .precio_unitario(new BigDecimal("20.00"))
                .subtotal(new BigDecimal("40.00"))
                .eliminado(false)
                .idAlimento(alimento)
                .idPedido(pedido)
                .build();
        pedido.setDetalles(List.of(detalle));
        return pedido;
    }
}
