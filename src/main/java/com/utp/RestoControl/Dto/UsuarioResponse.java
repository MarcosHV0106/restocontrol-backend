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

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                RolResponse.from(usuario.getRol())
        );
    }
}
