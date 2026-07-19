package com.utp.RestoControl.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Dto.RetirarLoteRequest;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoteInsumoServiceTests {

    @Mock
    private LoteInsumoRepository loteRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LoteInsumoService service;

    @Test
    void registraIngresoYRecalculaElStockDesdeLosLotes() {
        Insumo insumo = insumo();
        LoteInsumo existente = lote(1, insumo, BigDecimal.valueOf(3));
        LoteInsumoRequest request = new LoteInsumoRequest();
        request.setCantidad(BigDecimal.valueOf(7));
        request.setFechaVencimiento(LocalDate.now().plusMonths(1));
        request.setReferencia("COMPRA-10");
        when(insumoRepository.findByIdInsumoAndEliminadoFalse(1)).thenReturn(Optional.of(insumo));
        when(loteRepository.save(any(LoteInsumo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(loteRepository.findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(1))
                .thenAnswer(invocacion -> List.of(existente,
                        ultimoLoteGuardado(insumo, BigDecimal.valueOf(7))));

        LoteInsumo creado = service.crear(1, request);

        assertDecimal("7", creado.getCantidadActual());
        assertDecimal("10", insumo.getStockActual());
        ArgumentCaptor<MovimientoInventario> movimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(movimiento.capture());
        assertEquals("INGRESO", movimiento.getValue().getTipoMovimiento());
        assertDecimal("7", movimiento.getValue().getCantidad());
        verify(insumoRepository).save(insumo);
    }

    @Test
    void retiraTodaLaExistenciaComoMermaYRecalculaStock() {
        Insumo insumo = insumo();
        LoteInsumo retirado = lote(1, insumo, BigDecimal.valueOf(7));
        LoteInsumo restante = lote(2, insumo, BigDecimal.valueOf(3));
        when(loteRepository.findByIdLoteAndEliminadoFalse(1)).thenReturn(Optional.of(retirado));
        when(loteRepository.findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(1))
                .thenReturn(List.of(retirado, restante));
        RetirarLoteRequest request = new RetirarLoteRequest();
        request.setMotivo("Producto vencido");

        service.retirar(1, request);

        assertDecimal("0", retirado.getCantidadActual());
        assertEquals("RETIRADO", retirado.getEstado());
        assertDecimal("3", insumo.getStockActual());
        ArgumentCaptor<MovimientoInventario> movimiento = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoRepository).save(movimiento.capture());
        assertEquals("MERMA", movimiento.getValue().getTipoMovimiento());
        assertDecimal("7", movimiento.getValue().getCantidad());
    }

    @Test
    void rechazaLotesNuevosSinFechaDeVencimiento() {
        LoteInsumoRequest request = new LoteInsumoRequest();
        request.setCantidad(BigDecimal.valueOf(2));

        assertThrows(IllegalArgumentException.class, () -> service.crear(1, request));
    }

    private Insumo insumo() {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(1);
        insumo.setNombreInsumo("Pollo");
        insumo.setUnidadMedida("kg");
        insumo.setStockActual(BigDecimal.valueOf(3));
        insumo.setStockMinimo(BigDecimal.valueOf(5));
        insumo.setEliminado(false);
        return insumo;
    }

    private LoteInsumo lote(Integer id, Insumo insumo, BigDecimal cantidad) {
        LoteInsumo lote = ultimoLoteGuardado(insumo, cantidad);
        lote.setIdLote(id);
        lote.setCodigo("LOT-" + id);
        return lote;
    }

    private LoteInsumo ultimoLoteGuardado(Insumo insumo, BigDecimal cantidad) {
        LoteInsumo lote = new LoteInsumo();
        lote.setInsumo(insumo);
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        lote.setFechaIngreso(LocalDate.now());
        lote.setFechaVencimiento(LocalDate.now().plusMonths(1));
        lote.setEstado("ACTIVO");
        lote.setEliminado(false);
        return lote;
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
