package com.utp.RestoControl.Config;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Servicio personalizado para cargar detalles de usuario desde la base de datos
 * Implementa UserDetailsService de Spring Security
 */
@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Carga un usuario por su correo (nombre de usuario en Spring Security)
     * 
     * @param username El correo del usuario
     * @return UserDetails del usuario
     * @throws UsernameNotFoundException Si el usuario no es encontrado o está inactivo
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCaseAndEliminadoFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

            System.out.println("Encontrado: " + usuario.getCorreo());
            System.out.println("Disponible: " + usuario.getDisponible());
            System.out.println("Hash: " + usuario.getClave());

        // Verificar que el usuario esté disponible/activo
        if (!usuario.getDisponible()) {
            throw new UsernameNotFoundException("Usuario inactivo o no disponible: " + username);
        }

        // Construir las autoridades (roles)
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (usuario.getRol() != null) {
            // Agregar el rol del usuario con prefijo ROLE_
            String roleName = usuario.getRol().getNombreRol().toUpperCase().trim();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        } else {
            // Rol por defecto si no tiene rol asignado
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Crear y retornar el UserDetails
        // Nota: Los métodos accountNonExpired(), accountNonLocked(), credentialsNonExpired(), disabled()
        // han sido removidos en Spring Security 6.x. Solo usamos username, password y authorities.
        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getClave())
                .authorities(authorities)
                .build();
    }
}
