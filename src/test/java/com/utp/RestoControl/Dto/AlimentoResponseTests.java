package com.utp.RestoControl.Dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.RecetaAlimento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlimentoResponseTests {

    @Test
    void marcaComoPendienteUnPlatoHeredadoSinReceta() {
        Alimento alimento = alimentoBase();
        alimento.setReceta(new ArrayList<>());

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertFalse(response.getRecetaConfigurada());
        assertFalse(response.getDisponibleParaPedidos());
        assertEquals("Receta pendiente de configuracion", response.getMotivoNoDisponible());
    }

    @Test
    void calculaLasPorcionesDisponiblesDesdeElInsumoLimitante() {
        Alimento alimento = alimentoBase();
        Insumo insumo = new Insumo();
        insumo.setIdInsumo(5);
        insumo.setNombreInsumo("Pollo");
        insumo.setStockActual(new BigDecimal("10"));
        insumo.setCostoUnitario(new BigDecimal("8"));
        insumo.setEliminado(false);
        RecetaAlimento receta = new RecetaAlimento();
        receta.setAlimento(alimento);
        receta.setInsumo(insumo);
        receta.setCantidad(new BigDecimal("2"));
        alimento.setReceta(List.of(receta));

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertTrue(response.getRecetaConfigurada());
        assertTrue(response.getInventarioSuficiente());
        assertTrue(response.getDisponibleParaPedidos());
        assertEquals(5, response.getPorcionesDisponibles());
    }

    @Test
    void elBloqueoDeCocinaImpideVenderAunqueExistaInventario() {
        Alimento alimento = alimentoBase();
        Insumo insumo = new Insumo();
        insumo.setStockActual(new BigDecimal("10"));
        insumo.setEliminado(false);
        RecetaAlimento receta = new RecetaAlimento();
        receta.setAlimento(alimento);
        receta.setInsumo(insumo);
        receta.setCantidad(new BigDecimal("2"));
        alimento.setReceta(List.of(receta));
        alimento.setBloqueadoCocina(true);
        alimento.setMotivoBloqueoCocina("Horno fuera de servicio");

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertTrue(response.getInventarioSuficiente());
        assertFalse(response.getDisponibleParaPedidos());
        assertEquals(5, response.getPorcionesDisponibles());
        assertEquals("Cocina notifico: Horno fuera de servicio", response.getMotivoNoDisponible());
    }

    private Alimento alimentoBase() {
        CategoriaAlimento categoria = new CategoriaAlimento();
        categoria.setIdCategoria(1);
        categoria.setNombreCategoria("Fondos");
        Alimento alimento = new Alimento();
        alimento.setIdAlimento(2);
        alimento.setNombreAlimento("Pollo a la plancha");
        alimento.setPrecio(new BigDecimal("25"));
        alimento.setDisponible(true);
        alimento.setEliminado(false);
        alimento.setCategoria(categoria);
        return alimento;
    }
}
