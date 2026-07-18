package com.utp.RestoControl.Dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoteInsumoRequest {
    private BigDecimal cantidad;
    private LocalDate fechaVencimiento;
    private String referencia;
}
