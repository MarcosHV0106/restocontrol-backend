package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseAndEliminadoFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Verificar que el usuario esté disponible/activo
        if (Boolean.FALSE.equals(usuario.getDisponible())) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        if (Boolean.TRUE.equals(usuario.getPendiente())) {
            throw new UsernameNotFoundException("Cuenta pendiente de activacion: " + username);
        }

        if (usuario.getRol() == null || Boolean.TRUE.equals(usuario.getRol().getEliminado())) {
            throw new UsernameNotFoundException("Rol inactivo: " + username);
        }

        // Simplemente retornas tu clase UserPrincipal.
        // UserPrincipal ya se encarga de asignar los roles (authorities) en su constructor.
        return new UserPrincipal(usuario);
    }
}
