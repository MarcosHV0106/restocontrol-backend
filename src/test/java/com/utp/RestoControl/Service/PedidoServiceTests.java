package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.DetallePedidoRequest;
import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private DetallePedidoRepository detalleRepository;
    @Mock
    private MesaService mesaService;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private EstadoPedidoService estadoPedidoService;
    @Mock
    private ModalidadPedidoService modalidadPedidoService;
    @Mock
    private AlimentoService alimentoService;
    @Mock
    private StockAlimentoService stockAlimentoService;

    @InjectMocks
    private PedidoService pedidoService;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void administradorListaPedidosDeTodosLosUsuarios() {
        autenticar(1, "ADMIN");
        List<Pedido> esperados = List.of(new Pedido(), new Pedido());
        when(pedidoRepository.findActivosConRelaciones()).thenReturn(esperados);

        List<Pedido> resultado = pedidoService.listarPedidosSegunRol();

        assertSame(esperados, resultado);
        verify(pedidoRepository, never()).findActivosConRelacionesByUsuario(any());
    }

    @Test
    void meseroListaUnicamenteSusPedidos() {
        autenticar(7, "MESERO");
        List<Pedido> esperados = List.of(new Pedido());
        when(pedidoRepository.findActivosConRelacionesByUsuario(7)).thenReturn(esperados);

        List<Pedido> resultado = pedidoService.listarPedidosSegunRol();

        assertSame(esperados, resultado);
        verify(pedidoRepository, never()).findActivosConRelaciones();
    }

    @Test
    void meseroNoPuedeConsultarPedidoAjeno() {
        autenticar(7, "MESERO");
        Pedido pedidoAjeno = pedidoConCreador(15);
        when(pedidoRepository.findByIdPedidoAndEliminadoFalse(20)).thenReturn(Optional.of(pedidoAjeno));

        assertThrows(AccessDeniedException.class, () -> pedidoService.buscarPorIdSegunRol(20));
    }

    @Test
    void administradorPuedeConsultarPedidoAjeno() {
        autenticar(1, "ADMIN");
        Pedido pedidoAjeno = pedidoConCreador(15);
        when(pedidoRepository.findByIdPedidoAndEliminadoFalse(20)).thenReturn(Optional.of(pedidoAjeno));

        Pedido resultado = pedidoService.buscarPorIdSegunRol(20);

        assertSame(pedidoAjeno, resultado);
    }

    @Test
    void actualizarPedidoConservaElCreadorOriginalYRecalculaElTotal() {
        autenticar(7, "MESERO");
        Usuario creador = new Usuario();
        creador.setIdUsuario(7);

        Mesa mesa = new Mesa();
        mesa.setIdMesa(3);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(20);
        pedido.setUsuario(creador);
        pedido.setIdMesa(mesa);
        pedido.setEstadoPedido(new EstadoPedido(1, "PENDIENTE", false));

        ModalidadPedido modalidad = new ModalidadPedido(1, "MESA", false);
        pedido.setModalidadPedido(modalidad);
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(5);
        alimento.setPrecio(new BigDecimal("12.50"));

        PedidoRequest request = new PedidoRequest(
                3,
                999,
                1,
                1,
                "  Sin cebolla  ",
                List.of(new DetallePedidoRequest(5, 2))
        );

        when(pedidoRepository.findActivoParaGestion(20)).thenReturn(Optional.of(pedido));
        when(modalidadPedidoService.buscarPorId(1)).thenReturn(modalidad);
        when(detalleRepository.findByIdPedido_IdPedidoAndEliminadoFalse(20)).thenReturn(List.of());
        when(alimentoService.buscarPorId(5)).thenReturn(alimento);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido actualizado = pedidoService.actualizar(20, request);

        assertSame(creador, actualizado.getUsuario());
        assertEquals("Sin cebolla", actualizado.getObservacion());
        assertEquals(new BigDecimal("25.00"), actualizado.getTotal());
        assertEquals(1, actualizado.getDetalles().size());
        assertEquals(7, actualizado.getUsuario().getIdUsuario());
    }

    @Test
    void cajeroCreaPedidoParaLlevarComoBorradorSinMesa() {
        autenticar(3, "CAJERO");
        ModalidadPedido modalidad = new ModalidadPedido(2, "Para llevar", false);
        Usuario cajero = new Usuario();
        cajero.setIdUsuario(3);
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(5);
        alimento.setPrecio(new BigDecimal("18.00"));

        PedidoRequest request = new PedidoRequest(
                null,
                null,
                2,
                null,
                "Sin cubiertos",
                "Ana Torres",
                "987654321",
                null,
                List.of(new DetallePedidoRequest(5, 2))
        );

        when(modalidadPedidoService.buscarPorId(2)).thenReturn(modalidad);
        when(usuarioService.buscarPorId(3)).thenReturn(cajero);
        when(estadoPedidoService.buscarPorId(1)).thenReturn(new EstadoPedido(1, "PENDIENTE", false));
        when(alimentoService.buscarPorId(5)).thenReturn(alimento);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido creado = pedidoService.guardar(request);

        assertNull(creado.getIdMesa());
        assertNull(creado.getFechaEnvioCocina());
        assertEquals("Ana Torres", creado.getClienteNombre());
        assertEquals(new BigDecimal("36.00"), creado.getTotal());
        verify(mesaService, never()).ocuparParaPedido(any());
    }

    @Test
    void enviaBorradorACocinaYBloqueaSusProductos() {
        autenticar(7, "MESERO");
        Pedido pedido = pedidoGestionable("PENDIENTE");
        DetallePedido detalle = new DetallePedido();
        detalle.setEliminado(false);
        pedido.setDetalles(List.of(detalle));
        when(pedidoRepository.findActivoParaGestion(20)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido enviado = pedidoService.enviarACocina(20);

        assertNotNull(enviado.getFechaEnvioCocina());
        verify(stockAlimentoService).validarDisponibilidadParaPedido(pedido);

        PedidoRequest request = new PedidoRequest(3, null, 1, null, null, List.of(new DetallePedidoRequest(5, 1)));
        assertThrows(ConflictException.class, () -> pedidoService.actualizar(20, request));
    }

    @Test
    void solicitaCuentaSoloDespuesDeLaEntregaYMarcaLaMesaParaCobro() {
        autenticar(7, "MESERO");
        Pedido pedido = pedidoGestionable("ENTREGADO");
        pedido.setFechaEnvioCocina(LocalDateTime.now().minusMinutes(20));
        when(pedidoRepository.findActivoParaGestion(20)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido actualizado = pedidoService.solicitarCuenta(20);

        assertNotNull(actualizado.getFechaSolicitudCuenta());
        verify(mesaService).actualizarEstado(3, 4);
    }

    @Test
    void anulaPedidoAntesDePreparacionYLiberaLaMesa() {
        autenticar(7, "MESERO");
        Pedido pedido = pedidoGestionable("PENDIENTE");
        EstadoPedido cancelado = new EstadoPedido(8, "CANCELADO", false);
        when(pedidoRepository.findActivoParaGestion(20)).thenReturn(Optional.of(pedido));
        when(estadoPedidoService.listar()).thenReturn(List.of(cancelado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido anulado = pedidoService.anular(20, "Cliente se retiro");

        assertSame(cancelado, anulado.getEstadoPedido());
        assertNotNull(anulado.getFechaCancelacion());
        assertEquals("Cliente se retiro", anulado.getMotivoCancelacion());
        verify(mesaService).liberar(3);
    }

    private Pedido pedidoConCreador(Integer idUsuario) {
        Usuario creador = new Usuario();
        creador.setIdUsuario(idUsuario);
        Pedido pedido = new Pedido();
        pedido.setUsuario(creador);
        return pedido;
    }

    private Pedido pedidoGestionable(String estado) {
        Usuario creador = new Usuario();
        creador.setIdUsuario(7);
        Mesa mesa = new Mesa();
        mesa.setIdMesa(3);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(20);
        pedido.setUsuario(creador);
        pedido.setIdMesa(mesa);
        pedido.setEstadoPedido(new EstadoPedido(1, estado, false));
        pedido.setModalidadPedido(new ModalidadPedido(1, "MESA", false));
        pedido.setEliminado(false);
        return pedido;
    }

    private void autenticar(Integer idUsuario, String rol) {
        UserPrincipal principal = mock(UserPrincipal.class);
        if (!"ADMIN".equals(rol)) {
            when(principal.getId()).thenReturn(idUsuario);
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
