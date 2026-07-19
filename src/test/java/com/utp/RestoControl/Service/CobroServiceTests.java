package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.CobroRequest;
import com.utp.RestoControl.Dto.PagoCobroRequest;
import com.utp.RestoControl.Entity.Cobro;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.PagoCobro;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.CobroRepository;
import com.utp.RestoControl.Repository.PagoCobroRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
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
class CobroServiceTests {

    @Mock
    private CobroRepository cobroRepository;
    @Mock
    private PagoCobroRepository pagoCobroRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private EstadoPedidoService estadoPedidoService;
    @Mock
    private MesaService mesaService;

    @InjectMocks
    private CobroService cobroService;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void procesaCobroDivididoYLiberaLaMesa() {
        autenticar(1, "ADMIN");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        Usuario cajero = usuario(1);
        EstadoPedido pagado = new EstadoPedido(4, "PAGADO", false);

        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));
        when(cobroRepository.existsByPedido_IdPedidoAndEliminadoFalse(10)).thenReturn(false);
        when(usuarioService.buscarPorId(1)).thenReturn(cajero);
        when(cobroRepository.save(any(Cobro.class))).thenAnswer(invocacion -> {
            Cobro cobro = invocacion.getArgument(0);
            cobro.setIdCobro(30);
            return cobro;
        });
        when(pagoCobroRepository.saveAllAndFlush(anyList())).thenAnswer(invocacion -> {
            List<PagoCobro> pagos = invocacion.getArgument(0);
            for (int indice = 0; indice < pagos.size(); indice++) {
                pagos.get(indice).setIdPagoCobro(100 + indice);
            }
            return pagos;
        });
        when(pagoCobroRepository.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(estadoPedidoService.buscarPorId(4)).thenReturn(pagado);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(
                        pago("EFECTIVO", "25.00", "30.00", "BOLETA", "12345678", null),
                        pago("YAPE", "65.00", null, "BOLETA", null, null)
                )
        );

        Cobro cobro = cobroService.procesarCobro(10, request);

        assertEquals(new BigDecimal("76.27"), cobro.getSubtotal());
        assertEquals(new BigDecimal("13.73"), cobro.getIgv());
        assertEquals(new BigDecimal("90.00"), cobro.getTotalCobrado());
        assertEquals(new BigDecimal("95.00"), cobro.getTotalRecibido());
        assertEquals(new BigDecimal("5.00"), cobro.getVuelto());
        assertEquals("MIXTO", pedido.getMetodoPago());
        assertSame(pagado, pedido.getEstadoPedido());
        assertEquals("B001-00000100", cobro.getPagos().get(0).getNumeroComprobante());
        assertEquals("B001-00000101", cobro.getPagos().get(1).getNumeroComprobante());
        verify(mesaService).liberar(5);
    }

    @Test
    void rechazaCuandoLaSumaNoCoincideConElTotal() {
        autenticar(7, "MESERO");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));
        when(cobroRepository.existsByPedido_IdPedidoAndEliminadoFalse(10)).thenReturn(false);

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(pago("TARJETA", "80.00", null, "BOLETA", null, null))
        );

        assertThrows(IllegalArgumentException.class, () -> cobroService.procesarCobro(10, request));
        verify(cobroRepository, never()).save(any());
    }

    @Test
    void meseroNoPuedeCobrarPedidoAjeno() {
        autenticar(8, "MESERO");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(pago("TARJETA", "90.00", null, "BOLETA", null, null))
        );

        assertThrows(AccessDeniedException.class, () -> cobroService.procesarCobro(10, request));
        verify(cobroRepository, never()).save(any());
    }

    @Test
    void facturaRequiereRucYRazonSocialValidos() {
        autenticar(1, "ADMIN");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));
        when(cobroRepository.existsByPedido_IdPedidoAndEliminadoFalse(10)).thenReturn(false);

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(pago("TARJETA", "90.00", null, "FACTURA", "123", "Empresa SAC"))
        );

        assertThrows(IllegalArgumentException.class, () -> cobroService.procesarCobro(10, request));
    }

    @Test
    void rechazaPedidoQueYaTieneCobro() {
        autenticar(1, "ADMIN");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));
        when(cobroRepository.existsByPedido_IdPedidoAndEliminadoFalse(10)).thenReturn(true);

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(pago("TARJETA", "90.00", null, "BOLETA", null, null))
        );

        assertThrows(IllegalStateException.class, () -> cobroService.procesarCobro(10, request));
    }

    @Test
    void cajeroListaPedidosPendientesDeTodos() {
        autenticar(3, "CAJERO");
        List<Pedido> esperados = List.of(new Pedido(), new Pedido());
        when(pedidoRepository.findPendientesDeCobroConRelaciones()).thenReturn(esperados);

        assertSame(esperados, cobroService.listarPendientesSegunRol());
        verify(pedidoRepository, never()).findPendientesDeCobroPorUsuario(any());
    }

    @Test
    void meseroListaSoloSusPedidosPendientes() {
        autenticar(7, "MESERO");
        List<Pedido> esperados = List.of(new Pedido());
        when(pedidoRepository.findPendientesDeCobroPorUsuario(7)).thenReturn(esperados);

        assertSame(esperados, cobroService.listarPendientesSegunRol());
        verify(pedidoRepository, never()).findPendientesDeCobroConRelaciones();
    }

    @Test
    void rechazaCobroSiLaCuentaNoFueSolicitada() {
        autenticar(3, "CAJERO");
        Pedido pedido = pedidoPendiente(10, 7, "90.00");
        pedido.setFechaSolicitudCuenta(null);
        when(pedidoRepository.findActivoParaCobro(10)).thenReturn(Optional.of(pedido));

        CobroRequest request = new CobroRequest(
                BigDecimal.ZERO,
                List.of(pago("TARJETA", "90.00", null, "BOLETA", null, null))
        );

        assertThrows(IllegalStateException.class, () -> cobroService.procesarCobro(10, request));
        verify(cobroRepository, never()).save(any());
    }

    private Pedido pedidoPendiente(Integer idPedido, Integer idCreador, String total) {
        Mesa mesa = new Mesa();
        mesa.setIdMesa(5);
        mesa.setNumeroMesa(12);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(idPedido);
        pedido.setUsuario(usuario(idCreador));
        pedido.setIdMesa(mesa);
        pedido.setTotal(new BigDecimal(total));
        pedido.setEstadoPedido(new EstadoPedido(1, "PENDIENTE", false));
        pedido.setFechaSolicitudCuenta(java.time.LocalDateTime.now());
        pedido.setEliminado(false);
        return pedido;
    }

    private Usuario usuario(Integer idUsuario) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombre("Usuario");
        usuario.setApellido(String.valueOf(idUsuario));
        return usuario;
    }

    private PagoCobroRequest pago(
            String metodo,
            String monto,
            String recibido,
            String comprobante,
            String documento,
            String razonSocial
    ) {
        return new PagoCobroRequest(
                metodo,
                new BigDecimal(monto),
                recibido == null ? null : new BigDecimal(recibido),
                comprobante,
                documento,
                razonSocial,
                null
        );
    }

    private void autenticar(Integer idUsuario, String rol) {
        UserPrincipal principal = org.mockito.Mockito.mock(UserPrincipal.class);
        lenient().when(principal.getId()).thenReturn(idUsuario);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
