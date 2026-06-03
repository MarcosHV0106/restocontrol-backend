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
    private Boolean disponible;
    private CategoriaResponse categoria;

    public static AlimentoResponse from(Alimento alimento) {
        return new AlimentoResponse(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento(),
                alimento.getDescripcion(),
                alimento.getPrecio(),
                alimento.getDisponible(),
                CategoriaResponse.from(alimento.getCategoria())
        );
    }
}
