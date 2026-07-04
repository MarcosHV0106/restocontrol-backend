package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ActivacionCuentaResponse {

    private Boolean success;
    private String message;
    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private String estadoCuenta;

    public static ActivacionCuentaResponse pendiente(Usuario usuario) {
        return new ActivacionCuentaResponse(
                true,
                "Enlace de activacion valido.",
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                "PENDIENTE"
        );
    }

    public static ActivacionCuentaResponse activada(Usuario usuario) {
        return new ActivacionCuentaResponse(
                true,
                "Cuenta activada correctamente.",
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                "ACTIVO"
        );
    }
}
