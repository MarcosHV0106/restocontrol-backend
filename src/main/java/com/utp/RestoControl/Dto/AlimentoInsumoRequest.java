package com.utp.RestoControl.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlimentoInsumoRequest{

    private Integer idAlimento;

    private Integer idInsumo;

    private Double cantidadReferencial;

    private String unidadMedida;

    private String observacion;

}
