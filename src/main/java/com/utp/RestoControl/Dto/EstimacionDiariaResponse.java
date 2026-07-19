package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstimacionDiariaResponse(
        LocalDate fecha,
        boolean factible,
        int totalPorciones,
        List<PlatoEstimado> platos,
        List<InsumoRequerido> insumos
) {

    public record PlatoEstimado(
            Integer idAlimento,
            String alimento,
            String categoria,
            int porciones,
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
