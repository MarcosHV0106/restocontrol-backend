package com.utp.RestoControl.Dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AlimentoResponseTests {

    @Test
    void habilitaElPlatoCuandoTieneStock() {
        Alimento alimento = alimentoBase();
        alimento.setStock(8);

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertTrue(response.getDisponibleParaPedidos());
        assertEquals(8, response.getStock());
        assertNull(response.getMotivoNoDisponible());
    }

    @Test
    void bloqueaElPlatoCuandoNoTieneStock() {
        Alimento alimento = alimentoBase();
        alimento.setStock(0);

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertFalse(response.getDisponibleParaPedidos());
        assertEquals("Sin stock disponible", response.getMotivoNoDisponible());
    }

    @Test
    void respetaLaPausaTemporalDeCocina() {
        Alimento alimento = alimentoBase();
        alimento.setStock(10);
        alimento.setBloqueadoCocina(true);
        alimento.setMotivoBloqueoCocina("Horno fuera de servicio");

        AlimentoResponse response = AlimentoResponse.from(alimento);

        assertFalse(response.getDisponibleParaPedidos());
        assertEquals(10, response.getStock());
        assertEquals("Horno fuera de servicio", response.getMotivoNoDisponible());
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
        alimento.setBloqueadoCocina(false);
        alimento.setEliminado(false);
        alimento.setCategoria(categoria);
        return alimento;
    }
}
