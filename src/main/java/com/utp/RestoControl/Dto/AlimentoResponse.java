package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Alimento;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlimentoResponse {

    private Integer idAlimento;
    private String nombreAlimento;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal costoReceta;
    private Boolean disponible;
    private CategoriaResponse categoria;

    public static AlimentoResponse from(Alimento alimento) {
        return new AlimentoResponse(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento(),
                alimento.getDescripcion(),
                alimento.getPrecio(),
                calcularCostoReceta(alimento),
                alimento.getDisponible(),
                CategoriaResponse.from(alimento.getCategoria())
        );
    }

    private static BigDecimal calcularCostoReceta(Alimento alimento) {
        if (alimento.getReceta() == null) {
            return BigDecimal.ZERO;
        }

        return alimento.getReceta().stream()
                .filter(detalle -> detalle.getCantidad() != null
                        && detalle.getInsumo() != null
                        && detalle.getInsumo().getCostoUnitario() != null)
                .map(detalle -> detalle.getCantidad().multiply(detalle.getInsumo().getCostoUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
