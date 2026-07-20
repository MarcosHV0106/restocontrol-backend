package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EstimacionDiariaResponse(
        LocalDate fecha,
        boolean guardada,
        boolean guardable,
        boolean factible,
        int totalPorciones,
        int porcionesProcesadas,
        int porcionesPendientes,
        String responsable,
        LocalDateTime fechaActualizacion,
        List<PlatoEstimado> platos,
        List<InsumoRequerido> insumos
) {

    public record PlatoEstimado(
            Integer idAlimento,
            String alimento,
            String categoria,
            boolean planificable,
            int porciones,
            int porcionesProcesadas,
            int porcionesPendientes,
            String estado,
            String detalle
    ) {
    }

    public record InsumoRequerido(
            Integer idInsumo,
            String insumo,
            String unidadMedida,
            BigDecimal stockActual,
            BigDecimal cantidadRequerida,
            BigDecimal faltante
    ) {
    }
}
