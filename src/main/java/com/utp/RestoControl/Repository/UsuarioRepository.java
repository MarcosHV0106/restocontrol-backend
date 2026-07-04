
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<Usuario, Integer>{

    List<Usuario> findByEliminadoFalse();

    Optional<Usuario> findByIdUsuarioAndEliminadoFalse(Integer idUsuario);

    Optional<Usuario> findByCorreoIgnoreCaseAndEliminadoFalse(String correo);

    Optional<Usuario> findByTokenActivacionHashAndEliminadoFalse(String tokenActivacionHash);

    boolean existsByCorreoIgnoreCaseAndEliminadoFalse(String correo);

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNotAndEliminadoFalse(String correo, Integer idUsuario);
}
