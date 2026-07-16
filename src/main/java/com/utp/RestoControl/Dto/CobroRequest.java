package com.utp.RestoControl.Dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CobroRequest {

    private BigDecimal descuento;
    private List<PagoCobroRequest> pagos;
}
