package com.utp.RestoControl.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ReporteVentasResponse;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.Pedido;
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
class ReporteServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;
    @InjectMocks
    private ReporteService service;

    @Test
    void resumeLasVentasPagadasDelPeriodo() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(12);
        pedido.setFechaPedido(LocalDateTime.of(2026, 7, 10, 12, 0));
        pedido.setFechaPago(LocalDateTime.of(2026, 7, 10, 13, 30));
        pedido.setTotal(new BigDecimal("48.00"));
        pedido.setMetodoPago("YAPE");
        pedido.setClienteNombre("Maria");
        Mesa mesa = new Mesa();
        mesa.setNumeroMesa(4);
        pedido.setIdMesa(mesa);
        when(pedidoRepository.findVentasParaReporte(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(pedido));

        ReporteVentasResponse reporte = service.obtenerVentas(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertThat(reporte.ventasBrutasTotales()).isEqualByComparingTo("48.00");
        assertThat(reporte.totalTickets()).isEqualTo(1);
        assertThat(reporte.ticketPromedio()).isEqualByComparingTo("48.00");
        assertThat(reporte.metodosPago()).singleElement().satisfies(item -> {
            assertThat(item.metodo()).isEqualTo("Yape");
            assertThat(item.porcentaje()).isEqualByComparingTo("100.0");
        });
        assertThat(reporte.ultimasVentas()).singleElement().satisfies(item -> {
            assertThat(item.ticket()).isEqualTo("T-0012");
            assertThat(item.numeroMesa()).isEqualTo(4);
        });
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class, () -> service.obtenerVentas(
                LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 1)));
    }
}
