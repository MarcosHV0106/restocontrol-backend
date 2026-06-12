package com.utp.RestoControl.Config;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Servicio personalizado para cargar detalles de usuario desde la base de datos
 * Implementa UserDetailsService de Spring Security
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Carga un usuario por su nombre de usuario (correo en este caso)
     * 
     * @param username El correo del usuario
     * @return UserDetails del usuario
     * @throws UsernameNotFoundException Si el usuario no es encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Buscar usuario por correo (usamos correo como nombre de usuario)
        Usuario usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        // Verificar que el usuario esté activo
        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        // Construir las autoridades (roles)
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (usuario.getRol() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombreRol().toUpperCase()));
        } else {
            // Rol por defecto si no tiene rol asignado
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Crear y retornar el UserDetails
        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getClave())
                .authorities(authorities)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .disabled(!usuario.getActivo())
                .build();
    }
}
