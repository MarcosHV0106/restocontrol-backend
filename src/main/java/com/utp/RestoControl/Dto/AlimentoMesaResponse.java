package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Alimento;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlimentoMesaResponse {

    private Integer idAlimento;
    private String nombreAlimento;

    public static AlimentoMesaResponse from(Alimento alimento) {
        return new AlimentoMesaResponse(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento()
        );
    }
}
