package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.RecetaAlimento;
import java.math.BigDecimal;

public record RecetaAlimentoResponse(
        Integer idRecetaAlimento,
        Integer idInsumo,
        String nombreInsumo,
        String unidadMedida,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal subtotal
) {
    public static RecetaAlimentoResponse from(RecetaAlimento receta) {
        BigDecimal costo = receta.getInsumo().getCostoUnitario() == null
                ? BigDecimal.ZERO
                : receta.getInsumo().getCostoUnitario();
        return new RecetaAlimentoResponse(
                receta.getIdRecetaAlimento(),
                receta.getInsumo().getIdInsumo(),
                receta.getInsumo().getNombreInsumo(),
                receta.getInsumo().getUnidadMedida(),
                receta.getCantidad(),
                costo,
                receta.getCantidad().multiply(costo)
        );
    }
}
