package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Entity.AlertaInventario;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Repository.AlertaInventarioRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(alertaRepository.findByEliminadoFalseOrderByFechaGeneracionDesc()).thenReturn(List.of());
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
