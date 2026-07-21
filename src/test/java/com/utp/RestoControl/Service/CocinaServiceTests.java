package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.ActualizarDisponibilidadCocinaRequest;
import com.utp.RestoControl.Dto.AlimentoResponse;
import com.utp.RestoControl.Dto.HistorialTurnoCocinaResponse;
import com.utp.RestoControl.Dto.PedidoCocinaResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CocinaServiceTests {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private EstadoPedidoRepository estadoPedidoRepository;

    @Mock
    private StockAlimentoService stockAlimentoService;

    @Mock
    private AlimentoRepository alimentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

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

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
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
        verify(stockAlimentoService).descontarParaPedido(pedido);
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
        verify(stockAlimentoService, never()).descontarParaPedido(any(Pedido.class));
    }

    @Test
    void noRepiteElDescuentoSiElPedidoYaActualizoElStock() {
        Pedido pedido = pedidoConEstado(preparacion);
        pedido.setFechaDescuentoStock(java.time.LocalDateTime.now());
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));
        when(estadoPedidoRepository.findByEliminadoFalse()).thenReturn(List.of(listo));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        cocinaService.actualizarEstado(10, "LISTO");

        verify(stockAlimentoService, never()).descontarParaPedido(any(Pedido.class));
    }

    @Test
    void rechazaPedidoQueSigueEnBorrador() {
        Pedido pedido = pedidoConEstado(recibido);
        pedido.setFechaEnvioCocina(null);
        when(pedidoRepository.findActivoParaCocina(10)).thenReturn(Optional.of(pedido));

        assertThrows(ConflictException.class, () -> cocinaService.actualizarEstado(10, "EN_PREPARACION"));
        verify(stockAlimentoService, never()).descontarParaPedido(any(Pedido.class));
    }

    @Test
    void marcaProductoAgotadoYRegistraResponsable() {
        Alimento alimento = alimentoBase();
        Usuario cocinero = usuarioCocinero();
        autenticar(cocinero);
        ActualizarDisponibilidadCocinaRequest request = new ActualizarDisponibilidadCocinaRequest();
        request.setDisponible(false);
        request.setMotivo("Se terminó la salsa base");
        when(alimentoRepository.findActivoParaCocina(7)).thenReturn(Optional.of(alimento));
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(4)).thenReturn(Optional.of(cocinero));
        when(alimentoRepository.save(alimento)).thenReturn(alimento);

        AlimentoResponse response = cocinaService.actualizarDisponibilidadProducto(7, request);

        assertTrue(response.getBloqueadoCocina());
        assertEquals("Se terminó la salsa base", response.getMotivoBloqueoCocina());
        assertEquals("Ana Cocina", response.getResponsableBloqueoCocina());
        assertNotNull(response.getFechaBloqueoCocina());
        assertFalse(response.getDisponibleParaPedidos());
    }

    @Test
    void reactivaProductoSinCambiarLaConfiguracionAdministrativa() {
        Alimento alimento = alimentoBase();
        alimento.setDisponible(false);
        alimento.setBloqueadoCocina(true);
        alimento.setMotivoBloqueoCocina("Agotado");
        alimento.setFechaBloqueoCocina(LocalDateTime.now());
        alimento.setUsuarioBloqueoCocina(usuarioCocinero());
        ActualizarDisponibilidadCocinaRequest request = new ActualizarDisponibilidadCocinaRequest();
        request.setDisponible(true);
        when(alimentoRepository.findActivoParaCocina(7)).thenReturn(Optional.of(alimento));
        when(alimentoRepository.save(alimento)).thenReturn(alimento);

        AlimentoResponse response = cocinaService.actualizarDisponibilidadProducto(7, request);

        assertFalse(response.getBloqueadoCocina());
        assertFalse(response.getDisponible());
        assertFalse(response.getDisponibleParaPedidos());
    }

    @Test
    void estimaElTiempoDeUnPedidoConElHistoricoReal() {
        Pedido historico = pedidoCompleto(entregado, 2);
        historico.setFechaInicioPreparacion(LocalDateTime.now().minusMinutes(25));
        historico.setFechaListo(LocalDateTime.now().minusMinutes(5));
        Pedido activo = pedidoCompleto(recibido, 2);
        when(pedidoRepository.findPreparadosParaEstimacion(any(LocalDateTime.class)))
                .thenReturn(List.of(historico));
        when(pedidoRepository.findParaCocina(any(LocalDateTime.class))).thenReturn(List.of(activo));

        List<PedidoCocinaResponse> resultado = cocinaService.listarPedidos();

        assertEquals(1, resultado.size());
        assertEquals(20, resultado.getFirst().getTiempoEstimadoMinutos());
        assertNotNull(resultado.getFirst().getFechaEstimadaLista());
    }

    @Test
    void resumeLosPedidosPreparadosDelTurno() {
        Pedido entregadoTurno = pedidoCompleto(entregado, 3);
        entregadoTurno.setFechaInicioPreparacion(LocalDateTime.now().minusMinutes(30));
        entregadoTurno.setFechaListo(LocalDateTime.now().minusMinutes(12));
        entregadoTurno.setFechaEntregado(LocalDateTime.now().minusMinutes(10));
        when(pedidoRepository.findHistorialCocina(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(entregadoTurno));
        when(pedidoRepository.findPreparadosParaEstimacion(any(LocalDateTime.class)))
                .thenReturn(List.of(entregadoTurno));

        HistorialTurnoCocinaResponse resultado = cocinaService.obtenerHistorialTurno("ACTUAL");

        assertEquals(1, resultado.getTotalPedidos());
        assertEquals(3, resultado.getTotalPlatos());
        assertEquals(18, resultado.getPromedioPreparacionMinutos());
        assertEquals(1, resultado.getPedidos().size());
    }

    private Pedido pedidoConEstado(EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(10);
        pedido.setEstadoPedido(estado);
        pedido.setFechaEnvioCocina(LocalDateTime.now().minusMinutes(2));
        pedido.setEliminado(false);
        return pedido;
    }

    private Pedido pedidoCompleto(EstadoPedido estado, int cantidad) {
        Pedido pedido = pedidoConEstado(estado);
        Alimento alimento = alimentoBase();
        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1);
        detalle.setCantidad(cantidad);
        detalle.setIdAlimento(alimento);
        detalle.setIdPedido(pedido);
        detalle.setEliminado(false);
        pedido.setDetalles(List.of(detalle));
        pedido.setUsuario(usuarioCocinero());
        pedido.setModalidadPedido(new ModalidadPedido(1, "MESA", false));
        return pedido;
    }

    private Alimento alimentoBase() {
        CategoriaAlimento categoria = new CategoriaAlimento(1, "Fondos", null, false);
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(7);
        alimento.setNombreAlimento("Lomo saltado");
        alimento.setPrecio(new BigDecimal("28"));
        alimento.setDisponible(true);
        alimento.setBloqueadoCocina(false);
        alimento.setEliminado(false);
        alimento.setCategoria(categoria);
        alimento.setStock(20);
        return alimento;
    }

    private Usuario usuarioCocinero() {
        Rol rol = new Rol(6, "COCINERO", "Cocina", false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(4);
        usuario.setNombre("Ana");
        usuario.setApellido("Cocina");
        usuario.setCorreo("ana@restocontrol.test");
        usuario.setClave("clave");
        usuario.setRol(rol);
        usuario.setEliminado(false);
        return usuario;
    }

    private void autenticar(Usuario usuario) {
        UserPrincipal principal = new UserPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
