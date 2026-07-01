package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.EstadoMesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoMesaRepository
        extends JpaRepository<EstadoMesa, Integer> {

    List<EstadoMesa> findByEliminadoFalse();

    Optional<EstadoMesa> findByIdEstadoMesaAndEliminadoFalse(
            Integer idEstadoMesa);

    boolean existsByDescripcionAndEliminadoFalse(
            String descripcion);

    boolean existsByDescripcionAndIdEstadoMesaNotAndEliminadoFalse(
            String nombreEstado,
            Integer idEstadoMesa);

}
