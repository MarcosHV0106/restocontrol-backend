package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    List<Rol> findByEliminadoFalse();

    Optional<Rol> findByIdRolAndEliminadoFalse(Integer idRol);

    boolean existsByNombreRolIgnoreCaseAndEliminadoFalse(String nombreRol);

    boolean existsByNombreRolIgnoreCaseAndIdRolNotAndEliminadoFalse(String nombreRol, Integer idRol);
}
