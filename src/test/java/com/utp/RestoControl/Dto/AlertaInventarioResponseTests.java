package com.utp.RestoControl.Dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.utp.RestoControl.Entity.AlertaInventario;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AlertaInventarioResponseTests {

    @Test
    void exponeStockYCantidadSugeridaParaCerrarLaReposicion() {
        Insumo insumo = insumo(BigDecimal.valueOf(2), BigDecimal.valueOf(5));
        AlertaInventario alerta = alerta("STOCK_BAJO", insumo);

        AlertaInventarioResponse response = AlertaInventarioResponse.from(alerta);

        assertEquals(BigDecimal.valueOf(2), response.getStockActual());
        assertEquals(BigDecimal.valueOf(5), response.getStockMinimo());
        assertEquals(BigDecimal.valueOf(4), response.getCantidadSugeridaReposicion());
        assertEquals("kg", response.getUnidadMedida());
        assertNull(response.getCantidadLote());
    }

    @Test
    void exponeCantidadYVencimientoDelLoteRelacionado() {
        Insumo insumo = insumo(BigDecimal.TEN, BigDecimal.valueOf(5));
        LoteInsumo lote = new LoteInsumo();
        lote.setIdLote(8);
        lote.setCodigo("LOT-8");
        lote.setCantidadActual(BigDecimal.valueOf(3.5));
        lote.setFechaVencimiento(LocalDate.of(2026, 7, 20));

        AlertaInventario alerta = alerta("LOTE_VENCIDO", insumo);
        alerta.setLote(lote);

        AlertaInventarioResponse response = AlertaInventarioResponse.from(alerta);

        assertEquals(BigDecimal.valueOf(3.5), response.getCantidadLote());
        assertEquals(LocalDate.of(2026, 7, 20), response.getFechaVencimiento());
        assertNull(response.getCantidadSugeridaReposicion());
    }

    private Insumo insumo(BigDecimal stockActual, BigDecimal stockMinimo) {
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(3);
        insumo.setNombreInsumo("Pechuga de pollo");
        insumo.setUnidadMedida("kg");
        insumo.setStockActual(stockActual);
        insumo.setStockMinimo(stockMinimo);
        return insumo;
    }

    private AlertaInventario alerta(String tipo, Insumo insumo) {
        AlertaInventario alerta = new AlertaInventario();
        alerta.setIdAlerta(12);
        alerta.setTipo(tipo);
        alerta.setEstado("ACTIVA");
        alerta.setDetalle("Detalle de prueba");
        alerta.setInsumo(insumo);
        alerta.setFechaGeneracion(LocalDateTime.of(2026, 7, 20, 8, 30));
        alerta.setEliminado(false);
        return alerta;
    }
}
