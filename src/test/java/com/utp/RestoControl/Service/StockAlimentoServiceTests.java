package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockAlimentoServiceTests {

    @Mock
    private AlimentoRepository alimentoRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @InjectMocks
    private StockAlimentoService service;

    @Test
    void descuentaLaCantidadTotalDelPedido() {
        Alimento lomo = alimento(1, "Lomo saltado", 10);
        Pedido pedido = pedido(detalle(lomo, 2), detalle(lomo, 3));
        when(alimentoRepository.findActivoParaActualizarStock(1)).thenReturn(Optional.of(lomo));

        service.descontarParaPedido(pedido);

        assertEquals(5, lomo.getStock());
        assertNotNull(pedido.getFechaDescuentoStock());
        verify(alimentoRepository).save(lomo);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void rechazaElPedidoSiNoHayStockSuficiente() {
        Alimento ceviche = alimento(2, "Ceviche", 2);
        Pedido pedido = pedido(detalle(ceviche, 3));
        when(alimentoRepository.findByIdAlimentoAndEliminadoFalse(2)).thenReturn(Optional.of(ceviche));

        assertThrows(ConflictException.class, () -> service.validarDisponibilidadParaPedido(pedido));
    }

    @Test
    void noDescuentaDosVecesElMismoPedido() {
        Alimento plato = alimento(3, "Menu del dia", 8);
        Pedido pedido = pedido(detalle(plato, 2));
        pedido.setFechaDescuentoStock(LocalDateTime.now());

        service.descontarParaPedido(pedido);

        assertEquals(8, plato.getStock());
        verify(alimentoRepository, never()).findActivoParaActualizarStock(3);
        verify(pedidoRepository, never()).save(pedido);
    }

    private Alimento alimento(Integer id, String nombre, int stock) {
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(id);
        alimento.setNombreAlimento(nombre);
        alimento.setStock(stock);
        alimento.setDisponible(true);
        alimento.setBloqueadoCocina(false);
        alimento.setEliminado(false);
        return alimento;
    }

    private DetallePedido detalle(Alimento alimento, int cantidad) {
        DetallePedido detalle = new DetallePedido();
        detalle.setIdAlimento(alimento);
        detalle.setCantidad(cantidad);
        detalle.setEliminado(false);
        return detalle;
    }

    private Pedido pedido(DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(20);
        pedido.setDetalles(List.of(detalles));
        return pedido;
    }
}
