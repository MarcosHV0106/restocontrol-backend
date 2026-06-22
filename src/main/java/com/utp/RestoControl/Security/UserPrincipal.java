package com.utp.RestoControl.Security;

import com.utp.RestoControl.Entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {
    private final Integer id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Usuario usuario) {
        this.id = usuario.getIdUsuario();
        this.username = usuario.getCorreo();
        this.password = usuario.getClave();

        String nombreRol = usuario.getRol().getNombreRol();

        this.authorities = List.of(new SimpleGrantedAuthority(("ROLE_" + nombreRol).toUpperCase()));
    }

    public Integer getId() { return id; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}