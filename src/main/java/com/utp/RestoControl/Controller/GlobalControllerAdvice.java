package com.utp.RestoControl.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UsuarioRepository usuarioRepository;

    @ModelAttribute("nombreUsuario")
    public String getNombreUsuario() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null &&
                auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {

            String correo = auth.getName();

            return usuarioRepository
                    .findByCorreoIgnoreCaseAndEliminadoFalse(correo)
                    .map(u -> {
                        String nombre = u.getNombre().trim();
                        return nombre.contains(" ")
                                ? nombre.substring(0, nombre.indexOf(" "))
                                : nombre;
                    })
                    .orElse(correo);
        }

        return "Invitado";
    }

    @ModelAttribute("rolUsuario")
    public String getRolUsuario() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null &&
                auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal())) {

            String correo = auth.getName();

            return usuarioRepository
                    .findByCorreoIgnoreCaseAndEliminadoFalse(correo)
                    .map(u -> u.getRol().getNombreRol())
                    .orElse("Sin Rol");
        }

        return "Sin Rol";
    }
}