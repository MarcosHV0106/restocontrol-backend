
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<Usuario, Integer>{

    @EntityGraph(attributePaths = "rol")
    List<Usuario> findByEliminadoFalse();

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByIdUsuarioAndEliminadoFalse(Integer idUsuario);

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByCorreoIgnoreCaseAndEliminadoFalse(String correo);

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findByTokenActivacionHashAndEliminadoFalse(String tokenActivacionHash);

    boolean existsByCorreoIgnoreCaseAndEliminadoFalse(String correo);

    boolean existsByCorreoIgnoreCaseAndIdUsuarioNotAndEliminadoFalse(String correo, Integer idUsuario);
}
