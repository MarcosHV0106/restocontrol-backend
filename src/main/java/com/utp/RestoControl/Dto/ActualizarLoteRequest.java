package com.utp.RestoControl.Dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarLoteRequest {
    private String codigo;
    private LocalDate fechaVencimiento;
}
