package com.utp.RestoControl.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String mensaje;
    private UsuarioResponse usuario;
}
