package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.AtencionAlertaRequest;
import com.utp.RestoControl.Dto.RetirarLoteRequest;
import com.utp.RestoControl.Entity.AlertaInventario;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.AlertaInventarioRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AlertaInventarioServiceTests {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Mock
    private AlertaInventarioRepository alertaRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private LoteInsumoRepository loteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private LoteInsumoService loteService;

    @InjectMocks
    private AlertaInventarioService service;

    @BeforeEach
    void prepararAlertasExistentes() {
        lenient().when(alertaRepository.findByEliminadoFalseOrderByFechaGeneracionDesc()).thenReturn(List.of());
    }

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generaStockBajoCuandoEstaPorDebajoOIgualAlMinimo() {
        Insumo debajo = insumo(1, "Pollo", 4, 5);
        Insumo igual = insumo(2, "Leche", 5, 5);
        Insumo encima = insumo(3, "Tomate", 6, 5);
        when(insumoRepository.findByEliminadoFalse()).thenReturn(List.of(debajo, igual, encima));
        when(loteRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(alertaRepository.findByClaveActivaAndEliminadoFalse(anyString())).thenReturn(Optional.empty());

        service.sincronizar();

        ArgumentCaptor<AlertaInventario> captor = ArgumentCaptor.forClass(AlertaInventario.class);
        verify(alertaRepository, atLeastOnce()).save(captor.capture());
        List<AlertaInventario> stock = captor.getAllValues().stream()
                .filter(alerta -> AlertaInventarioService.STOCK_BAJO.equals(alerta.getTipo()))
                .toList();
        assertEquals(2, stock.size());
        assertEquals(List.of(1, 2), stock.stream().map(a -> a.getInsumo().getIdInsumo()).toList());
    }

    @Test
    void clasificaVencidoHoySieteDiasYFueraDelRango() {
        LocalDate hoy = LocalDate.now(ZONA_LIMA);
        Insumo insumo = insumo(1, "Pollo", 20, 5);
        List<LoteInsumo> lotes = List.of(
                lote(1, insumo, hoy.minusDays(1)),
                lote(2, insumo, hoy),
                lote(3, insumo, hoy.plusDays(7)),
                lote(4, insumo, hoy.plusDays(8))
        );
        when(insumoRepository.findByEliminadoFalse()).thenReturn(List.of(insumo));
        when(loteRepository.findByEliminadoFalse()).thenReturn(lotes);
        when(alertaRepository.findByClaveActivaAndEliminadoFalse(anyString())).thenReturn(Optional.empty());

        service.sincronizar();

        ArgumentCaptor<AlertaInventario> captor = ArgumentCaptor.forClass(AlertaInventario.class);
        verify(alertaRepository, atLeastOnce()).save(captor.capture());
        assertEquals(1, captor.getAllValues().stream()
                .filter(a -> AlertaInventarioService.LOTE_VENCIDO.equals(a.getTipo())).count());
        assertEquals(2, captor.getAllValues().stream()
                .filter(a -> AlertaInventarioService.VENCIMIENTO_PROXIMO.equals(a.getTipo())).count());
    }

    @Test
    void reutilizaLaAlertaActivaYNoCreaDuplicados() {
        Insumo insumo = insumo(1, "Pollo", 3, 5);
        AlertaInventario existente = alertaActiva(insumo);
        LocalDateTime fechaOriginal = existente.getFechaGeneracion();
        when(insumoRepository.findByEliminadoFalse()).thenReturn(List.of(insumo));
        when(loteRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(alertaRepository.findByClaveActivaAndEliminadoFalse("STOCK_BAJO:INSUMO:1"))
                .thenReturn(Optional.of(existente));

        service.sincronizar();
        service.sincronizar();

        verify(alertaRepository, atLeastOnce()).save(existente);
        assertSame(insumo, existente.getInsumo());
        assertEquals(fechaOriginal, existente.getFechaGeneracion());
    }

    @Test
    void cierraAutomaticamenteLaAlertaCuandoSeReponeElStock() {
        Insumo insumo = insumo(1, "Pollo", 6, 5);
        AlertaInventario existente = alertaActiva(insumo);
        when(insumoRepository.findByEliminadoFalse()).thenReturn(List.of(insumo));
        when(loteRepository.findByEliminadoFalse()).thenReturn(List.of());
        when(alertaRepository.findByClaveActivaAndEliminadoFalse("STOCK_BAJO:INSUMO:1"))
                .thenReturn(Optional.of(existente));

        service.sincronizar();

        assertEquals("ATENDIDA", existente.getEstado());
        assertNull(existente.getClaveActiva());
        verify(alertaRepository).save(existente);
    }

    @Test
    void solicitarReposicionDejaLaAlertaEnSeguimientoConAuditoria() {
        Insumo insumo = insumo(1, "Pollo", 2, 5);
        AlertaInventario alerta = alertaActiva(insumo);
        Usuario responsable = autenticarUsuario(7);
        when(alertaRepository.findByIdAlertaAndEliminadoFalse(10)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(any(AlertaInventario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        AtencionAlertaRequest request = new AtencionAlertaRequest();
        request.setAccion("SOLICITAR_REPOSICION");
        request.setObservacion("Comprar antes del turno noche");

        AlertaInventario atendida = service.atender(10, request);

        assertEquals("REVISADA", atendida.getEstado());
        assertEquals("SOLICITAR_REPOSICION", atendida.getAccion());
        assertEquals("Comprar antes del turno noche", atendida.getObservacion());
        assertSame(responsable, atendida.getUsuarioAtencion());
        assertNotNull(atendida.getFechaRevision());
        assertNull(atendida.getFechaAtencion());
    }

    @Test
    void retirarLoteVencidoRegistraMermaYCierraLaAlerta() {
        Insumo insumo = insumo(1, "Pollo", 10, 5);
        LoteInsumo lote = lote(22, insumo, LocalDate.now(ZONA_LIMA).minusDays(1));
        AlertaInventario alerta = alertaActiva(insumo);
        alerta.setTipo(AlertaInventarioService.LOTE_VENCIDO);
        alerta.setLote(lote);
        alerta.setClaveActiva("LOTE_VENCIDO:LOTE:22");
        autenticarUsuario(7);
        when(alertaRepository.findByIdAlertaAndEliminadoFalse(10)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(any(AlertaInventario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        AtencionAlertaRequest request = new AtencionAlertaRequest();
        request.setAccion("RETIRAR_LOTE_MERMA");
        request.setObservacion("Lote descartado por vencimiento");

        AlertaInventario atendida = service.atender(10, request);

        ArgumentCaptor<RetirarLoteRequest> retiro = ArgumentCaptor.forClass(RetirarLoteRequest.class);
        verify(loteService).retirar(eq(22), retiro.capture());
        assertEquals("Lote descartado por vencimiento", retiro.getValue().getMotivo());
        assertEquals("ALERTA-10", retiro.getValue().getReferencia());
        assertEquals("ATENDIDA", atendida.getEstado());
        assertNotNull(atendida.getFechaAtencion());
        assertNull(atendida.getClaveActiva());
    }

    private Usuario autenticarUsuario(Integer idUsuario) {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(idUsuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setNombre("María");
        usuario.setApellido("Almacén");
        when(usuarioRepository.findByIdUsuarioAndEliminadoFalse(idUsuario)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private Insumo insumo(Integer id, String nombre, double stock, double minimo) {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(id);
        insumo.setNombreInsumo(nombre);
        insumo.setUnidadMedida("kg");
        insumo.setStockActual(BigDecimal.valueOf(stock));
        insumo.setStockMinimo(BigDecimal.valueOf(minimo));
        insumo.setEliminado(false);
        return insumo;
    }

    private LoteInsumo lote(Integer id, Insumo insumo, LocalDate vencimiento) {
        LoteInsumo lote = new LoteInsumo();
        lote.setIdLote(id);
        lote.setInsumo(insumo);
        lote.setCodigo("LOT-" + id);
        lote.setCantidadInicial(BigDecimal.TEN);
        lote.setCantidadActual(BigDecimal.TEN);
        lote.setFechaIngreso(LocalDate.now(ZONA_LIMA));
        lote.setFechaVencimiento(vencimiento);
        lote.setEstado("ACTIVO");
        lote.setEliminado(false);
        return lote;
    }

    private AlertaInventario alertaActiva(Insumo insumo) {
        AlertaInventario alerta = new AlertaInventario();
        alerta.setIdAlerta(10);
        alerta.setTipo(AlertaInventarioService.STOCK_BAJO);
        alerta.setEstado("ACTIVA");
        alerta.setDetalle("Stock bajo");
        alerta.setInsumo(insumo);
        alerta.setFechaGeneracion(LocalDateTime.now().minusHours(1));
        alerta.setClaveActiva("STOCK_BAJO:INSUMO:" + insumo.getIdInsumo());
        alerta.setEliminado(false);
        return alerta;
    }
}
