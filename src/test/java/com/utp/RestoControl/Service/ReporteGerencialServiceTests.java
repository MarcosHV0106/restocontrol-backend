package com.utp.RestoControl.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ReporteGerencialResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporteGerencialServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private AlimentoRepository alimentoRepository;
    @InjectMocks
    private ReporteGerencialService service;

    @Test
    void consolidaVentasProductosModalidadesYCancelaciones() {
        CategoriaAlimento categoria = new CategoriaAlimento(1, "Fondos", null, false);
        Alimento lomo = new Alimento();
        lomo.setIdAlimento(3);
        lomo.setNombreAlimento("Lomo saltado");
        lomo.setCategoria(categoria);
        lomo.setEliminado(false);

        Pedido venta = pedido(20, "60.00", "MESA");
        venta.setFechaPago(LocalDateTime.of(2026, 7, 12, 14, 0));
        DetallePedido detalle = new DetallePedido();
        detalle.setIdAlimento(lomo);
        detalle.setCantidad(2);
        detalle.setSubtotal(new BigDecimal("60.00"));
        detalle.setEliminado(false);
        venta.setDetalles(List.of(detalle));

        Pedido cancelado = pedido(21, "30.00", "PARA LLEVAR");
        cancelado.setFechaCancelacion(LocalDateTime.of(2026, 7, 13, 15, 0));
        cancelado.setMotivoCancelacion("Cliente se retiro");

        when(pedidoRepository.findVentasParaReporte(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(venta));
        when(pedidoRepository.findCanceladosParaReporte(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cancelado));
        when(alimentoRepository.findByEliminadoFalse()).thenReturn(List.of(lomo));

        ReporteGerencialResponse reporte = service.obtener(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(reporte.indicadores().ventasTotales()).isEqualByComparingTo("60.00");
        assertThat(reporte.indicadores().pedidosPagados()).isEqualTo(1);
        assertThat(reporte.indicadores().pedidosCancelados()).isEqualTo(1);
        assertThat(reporte.indicadores().tasaCancelacion()).isEqualByComparingTo("50.0");
        assertThat(reporte.productosMasVendidos()).singleElement().satisfies(item -> {
            assertThat(item.producto()).isEqualTo("Lomo saltado");
            assertThat(item.cantidadVendida()).isEqualTo(2);
        });
        assertThat(reporte.ventasPorModalidad()).singleElement()
                .extracting(ReporteGerencialResponse.VentasModalidad::modalidad).isEqualTo("MESA");
        assertThat(reporte.pedidosCancelados()).singleElement()
                .extracting(ReporteGerencialResponse.PedidoCancelado::motivo).isEqualTo("Cliente se retiro");
    }

    private Pedido pedido(Integer id, String total, String modalidad) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        pedido.setFechaPedido(LocalDateTime.of(2026, 7, 12, 12, 0));
        pedido.setTotal(new BigDecimal(total));
        pedido.setModalidadPedido(new ModalidadPedido(id, modalidad, false));
        pedido.setEliminado(false);
        return pedido;
    }
}
