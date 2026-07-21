package com.utp.RestoControl.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.utp.RestoControl.Entity.CategoriaAlimento;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlimentoRequest {

    private String nombreAlimento;
    private String descripcion;
    private BigDecimal precio;
    private Boolean disponible;
    private Integer stock;

    @JsonAlias({"id_categoria", "categoriaId"})
    private Integer idCategoria;
    private CategoriaAlimento categoria;
}
