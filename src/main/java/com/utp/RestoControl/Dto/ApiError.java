package com.utp.RestoControl.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiError {

    private String mensaje;
    private int estado;
}
