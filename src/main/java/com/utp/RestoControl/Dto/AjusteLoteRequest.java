package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AjusteLoteRequest {
    private String tipo;
    private BigDecimal cantidad;
    private String motivo;
    private String referencia;
}
