package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CocinaServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstadoPedidoRepository estadoPedidoRepository;

    @InjectMocks
    private CocinaService cocinaService;

    private EstadoPedido recibido;
    private EstadoPedido preparacion;
    private EstadoPedido listo;
    private EstadoPedido entregado;

    @BeforeEach
    void prepararEstados() {
        recibido = new EstadoPedido(1, "PENDIENTE", false);
        preparacion = new EstadoPedido(2, "EN PREPARACIÓN", false);
        listo = new EstadoPedido(3, "LISTO", false);
        entregado = new EstadoPedido(5, "ENTREGADO", false);
    }

    @Test
    void iniciaPreparacionDesdeRecibido() {
        Pedido pedido = pedidoConEstado(recibido);
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));
        when(estadoPedidoRepository.findByEliminadoFalse()).thenReturn(List.of(recibido, preparacion, listo));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido resultado = cocinaService.actualizarEstado(10, "EN_PREPARACION");

        assertSame(preparacion, resultado.getEstadoPedido());
        assertNotNull(resultado.getFechaInicioPreparacion());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void marcaListoYConservaInicioDePreparacion() {
        Pedido pedido = pedidoConEstado(preparacion);
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));
        when(estadoPedidoRepository.findByEliminadoFalse()).thenReturn(List.of(recibido, preparacion, listo));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido resultado = cocinaService.actualizarEstado(10, "LISTO");

        assertSame(listo, resultado.getEstadoPedido());
        assertNotNull(resultado.getFechaInicioPreparacion());
        assertNotNull(resultado.getFechaListo());
    }

    @Test
    void marcaEntregadoUnicamenteDesdeListo() {
        Pedido pedido = pedidoConEstado(listo);
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));
        when(estadoPedidoRepository.findByEliminadoFalse()).thenReturn(List.of(entregado));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido resultado = cocinaService.actualizarEstado(10, "ENTREGADO");

        assertSame(entregado, resultado.getEstadoPedido());
        assertNotNull(resultado.getFechaListo());
        assertNotNull(resultado.getFechaEntregado());
    }

    @Test
    void rechazaSaltarEtapasConUnConflicto() {
        Pedido pedido = pedidoConEstado(recibido);
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));

        assertThrows(
                ConflictException.class,
                () -> cocinaService.actualizarEstado(10, "LISTO")
        );

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    private Pedido pedidoConEstado(EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10);
        pedido.setEstadoPedido(estado);
        pedido.setEliminado(false);
        return pedido;
    }
}
