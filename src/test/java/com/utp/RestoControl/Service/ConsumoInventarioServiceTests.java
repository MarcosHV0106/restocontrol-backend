package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import com.utp.RestoControl.Repository.RecetaAlimentoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConsumoInventarioServiceTests {

    @Mock
    private RecetaAlimentoRepository recetaRepository;
    @Mock
    private LoteInsumoRepository loteRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AlertaInventarioService alertaService;

    @InjectMocks
    private ConsumoInventarioService service;

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void agregaRecetasConsumeLotesPorVencimientoYRegistraTrazabilidad() {
        Insumo pollo = insumo(1, "Pollo", "kg", "20");
        Alimento plancha = alimento(1, "Pollo a la plancha");
        Alimento broaster = alimento(2, "Pollo broaster");
        Pedido pedido = pedido(10,
                detalle(plancha, 2),
                detalle(broaster, 4));
        LoteInsumo primero = lote(1, pollo, "5", LocalDate.now().plusDays(2));
        LoteInsumo segundo = lote(2, pollo, "10", LocalDate.now().plusDays(20));
        Usuario cocinero = autenticarCocinero(7);

        when(recetaRepository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(1))
                .thenReturn(List.of(receta(plancha, pollo, "2")));
        when(recetaRepository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(2))
                .thenReturn(List.of(receta(broaster, pollo, "1")));
        when(loteRepository.findConsumiblesParaPedido(any(Integer.class), any(LocalDate.class)))
                .thenReturn(List.of(primero, segundo));
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(7)).thenReturn(Optional.of(cocinero));

        service.consumirParaPedido(pedido);

        assertEquals("AGOTADO", primero.getEstado());
        assertDecimal("0", primero.getCantidadActual());
        assertDecimal("7", segundo.getCantidadActual());
        assertDecimal("7", pollo.getStockActual());
        assertNotNull(pedido.getFechaConsumoInventario());

        ArgumentCaptor<List<MovimientoInventario>> movimientos = ArgumentCaptor.forClass(List.class);
        verify(movimientoRepository).saveAll(movimientos.capture());
        assertEquals(2, movimientos.getValue().size());
        assertDecimal("5", movimientos.getValue().get(0).getCantidad());
        assertDecimal("3", movimientos.getValue().get(1).getCantidad());
        assertTrue(movimientos.getValue().stream().allMatch(movimiento ->
                ConsumoInventarioService.TIPO_CONSUMO_PEDIDO.equals(movimiento.getTipoMovimiento())
                        && "PEDIDO-10".equals(movimiento.getReferencia())
                        && movimiento.getPedido() == pedido
                        && movimiento.getUsuario() == cocinero));
        verify(alertaService).sincronizar();
    }

    @Test
    void noModificaNadaCuandoElStockUtilEsInsuficiente() {
        Insumo pollo = insumo(1, "Pollo", "kg", "20");
        Alimento plancha = alimento(1, "Pollo a la plancha");
        Pedido pedido = pedido(10, detalle(plancha, 4));
        LoteInsumo lote = lote(1, pollo, "5", LocalDate.now().plusDays(2));
        when(recetaRepository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(1))
                .thenReturn(List.of(receta(plancha, pollo, "2")));
        when(loteRepository.findConsumiblesParaPedido(any(Integer.class), any(LocalDate.class)))
                .thenReturn(List.of(lote));

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> service.consumirParaPedido(pedido)
        );

        assertTrue(error.getMessage().contains("3 kg de Pollo"));
        assertDecimal("5", lote.getCantidadActual());
        assertNull(pedido.getFechaConsumoInventario());
        verify(loteRepository, never()).saveAll(any());
        verify(movimientoRepository, never()).saveAll(any());
        verifyNoInteractions(alertaService);
    }

    @Test
    void bloqueaElPedidoSiUnPlatoNoTieneReceta() {
        Alimento ceviche = alimento(3, "Ceviche clásico");
        Pedido pedido = pedido(10, detalle(ceviche, 1));
        when(recetaRepository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(3))
                .thenReturn(List.of());

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> service.consumirParaPedido(pedido)
        );

        assertTrue(error.getMessage().contains("Ceviche clásico"));
        verifyNoInteractions(loteRepository, movimientoRepository, alertaService);
    }

    @Test
    void esIdempotenteCuandoElPedidoYaTieneConsumo() {
        Pedido pedido = pedido(10);
        pedido.setFechaConsumoInventario(LocalDateTime.now());

        service.consumirParaPedido(pedido);

        verifyNoInteractions(
                recetaRepository,
                loteRepository,
                insumoRepository,
                movimientoRepository,
                usuarioRepository,
                alertaService
        );
    }

    private Pedido pedido(Integer id, DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(id);
        pedido.setDetalles(List.of(detalles));
        for (DetallePedido detalle : detalles) {
            detalle.setIdPedido(pedido);
        }
        return pedido;
    }

    private DetallePedido detalle(Alimento alimento, int cantidad) {
        DetallePedido detalle = new DetallePedido();
        detalle.setIdAlimento(alimento);
        detalle.setCantidad(cantidad);
        detalle.setEliminado(false);
        return detalle;
    }

    private Alimento alimento(Integer id, String nombre) {
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(id);
        alimento.setNombreAlimento(nombre);
        return alimento;
    }

    private Insumo insumo(Integer id, String nombre, String unidad, String stock) {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(id);
        insumo.setNombreInsumo(nombre);
        insumo.setUnidadMedida(unidad);
        insumo.setStockActual(new BigDecimal(stock));
        insumo.setEliminado(false);
        return insumo;
    }

    private RecetaAlimento receta(Alimento alimento, Insumo insumo, String cantidad) {
        RecetaAlimento receta = new RecetaAlimento();
        receta.setAlimento(alimento);
        receta.setInsumo(insumo);
        receta.setCantidad(new BigDecimal(cantidad));
        return receta;
    }

    private LoteInsumo lote(Integer id, Insumo insumo, String cantidad, LocalDate vencimiento) {
        LoteInsumo lote = new LoteInsumo();
        lote.setIdLote(id);
        lote.setCodigo("LOT-" + id);
        lote.setInsumo(insumo);
        lote.setCantidadInicial(new BigDecimal(cantidad));
        lote.setCantidadActual(new BigDecimal(cantidad));
        lote.setFechaIngreso(LocalDate.now());
        lote.setFechaVencimiento(vencimiento);
        lote.setEstado("ACTIVO");
        lote.setEliminado(false);
        return lote;
    }

    private Usuario autenticarCocinero(Integer idUsuario) {
        Rol rol = new Rol(6, "COCINERO", "Cocina", false);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombre("Ana");
        usuario.setApellido("Cocina");
        usuario.setCorreo("cocina@local.test");
        usuario.setClave("hash");
        usuario.setRol(rol);
        usuario.setEliminado(false);
        UserPrincipal principal = new UserPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
        return usuario;
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
