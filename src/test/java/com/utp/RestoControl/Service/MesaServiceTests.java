package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Entity.EstadoMesa;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Repository.MesaRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MesaServiceTests {

    @Mock
    private MesaRepository repository;

    @Mock
    private EstadoMesaService estadoMesaService;

    @InjectMocks
    private MesaService mesaService;

    @Test
    void ocupaMesaLibreParaUnNuevoPedido() {
        EstadoMesa libre = estado(1, "libre");
        EstadoMesa ocupada = estado(2, "ocupada");
        Mesa mesa = mesa(4, libre);
        when(repository.findActivasParaActualizar(List.of(4))).thenReturn(List.of(mesa));
        when(estadoMesaService.buscarPorId(2)).thenReturn(ocupada);
        when(repository.save(mesa)).thenReturn(mesa);

        Mesa resultado = mesaService.ocuparParaPedido(4);

        assertSame(ocupada, resultado.getEstadoMesa());
    }

    @Test
    void rechazaMesaQueYaEstaOcupada() {
        Mesa mesa = mesa(4, estado(2, "ocupada"));
        when(repository.findActivasParaActualizar(List.of(4))).thenReturn(List.of(mesa));

        assertThrows(IllegalStateException.class, () -> mesaService.ocuparParaPedido(4));
    }

    @Test
    void transfierePedidoYActualizaAmbasMesasBajoBloqueo() {
        EstadoMesa libre = estado(1, "libre");
        EstadoMesa ocupada = estado(2, "ocupada");
        Mesa origen = mesa(4, ocupada);
        Mesa destino = mesa(7, libre);
        when(repository.findActivasParaActualizar(List.of(4, 7))).thenReturn(List.of(origen, destino));
        when(estadoMesaService.buscarPorId(1)).thenReturn(libre);
        when(estadoMesaService.buscarPorId(2)).thenReturn(ocupada);

        Mesa resultado = mesaService.transferirPedido(4, 7, false);

        assertSame(destino, resultado);
        assertSame(libre, origen.getEstadoMesa());
        assertSame(ocupada, destino.getEstadoMesa());
        verify(repository).saveAll(any());
    }

    @Test
    void liberaMesaAlCerrarLaAtencion() {
        EstadoMesa libre = estado(1, "libre");
        Mesa mesa = mesa(4, estado(4, "cobrar"));
        when(repository.findActivasParaActualizar(List.of(4))).thenReturn(List.of(mesa));
        when(estadoMesaService.buscarPorId(1)).thenReturn(libre);

        mesaService.liberar(4);

        assertSame(libre, mesa.getEstadoMesa());
        verify(repository).save(mesa);
    }

    private Mesa mesa(Integer id, EstadoMesa estado) {
        return new Mesa(id, id, 4, 1, estado, false);
    }

    private EstadoMesa estado(Integer id, String descripcion) {
        return new EstadoMesa(id, descripcion, false);
    }
}
