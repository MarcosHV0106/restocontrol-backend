package com.utp.RestoControl.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("nombreUsuario")
    public String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            /* * OPCIÓN A: Si tienes un UserDetails personalizado que envuelve tu entidad Usuario.
             * Descomenta esto si aplica a tu proyecto:
             * * MiUsuarioPrincipal userPrincipal = (MiUsuarioPrincipal) auth.getPrincipal();
             * return userPrincipal.getUsuario().getNombre(); 
             */
             
            // OPCIÓN B: Por defecto, si solo tienes el correo en el contexto, 
            // cortamos el texto antes del '@' para que sirva como nombre temporal.
            String username = auth.getName();
            return username.contains("@") ? username.split("@")[0] : username;
        }
        return "Invitado";
    }

    @ModelAttribute("rolUsuario")
    public String getRolUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !auth.getAuthorities().isEmpty()) {
            // Extrae el primer rol (ej. "ROLE_ADMINISTRADOR") y le quita el prefijo "ROLE_"
            GrantedAuthority authority = auth.getAuthorities().iterator().next();
            return authority.getAuthority().replace("ROLE_", "");
        }
        return "Sin Rol";
    }
}