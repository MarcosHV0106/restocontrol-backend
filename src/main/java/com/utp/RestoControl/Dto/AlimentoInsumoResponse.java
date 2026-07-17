package com.utp.RestoControl.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlimentoInsumoResponse {

    private Integer idAlimentoInsumo;

    private Integer idAlimento;

    private String nombreAlimento;

    private Integer idInsumo;

    private String nombreInsumo;

    private Double cantidadReferencial;

    private String unidadMedida;

    private String observacion;

}
