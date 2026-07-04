package com.utp.RestoControl.Dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivacionCuentaRequest {

    private String clave;

    @JsonAlias({"confirmarClave", "confirmacion", "confirmPassword"})
    private String confirmacionClave;
}
