package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.CategoriaAlimento;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoriaResponse {

    private Integer idCategoria;
    private String nombreCategoria;
    private String descripcion;

    public static CategoriaResponse from(CategoriaAlimento categoria) {
        if (categoria == null) {
            return null;
        }

        return new CategoriaResponse(
                categoria.getIdCategoria(),
                categoria.getNombreCategoria(),
                categoria.getDescripcion()
        );
    }
}
