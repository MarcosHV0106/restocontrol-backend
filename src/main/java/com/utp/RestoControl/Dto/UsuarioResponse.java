package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioResponse {

    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private RolResponse rol;
    private Boolean disponible;
    private Boolean pendiente;
    private String estadoCuenta;

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                RolResponse.from(usuario.getRol()),
                usuario.getDisponible(),
                Boolean.TRUE.equals(usuario.getPendiente()),
                resolverEstadoCuenta(usuario)
        );
    }

    private static String resolverEstadoCuenta(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getPendiente())) {
            return "PENDIENTE";
        }

        if (Boolean.FALSE.equals(usuario.getDisponible())) {
            return "INACTIVO";
        }

        return "ACTIVO";
    }
}
