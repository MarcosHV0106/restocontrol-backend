package com.utp.RestoControl.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResumenAlertasResponse {
    private long activas;
    private long revisadas;
    private long atendidas;
}
