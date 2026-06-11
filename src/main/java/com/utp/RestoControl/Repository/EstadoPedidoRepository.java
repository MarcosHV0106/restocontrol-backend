package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.EstadoPedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoPedidoRepository
        extends JpaRepository<EstadoPedido, Integer> {

    List<EstadoPedido> findByEliminadoFalse();

    Optional<EstadoPedido> findByIdEstadoPedidoAndEliminadoFalse(
            Integer idEstadoPedido);

    boolean existsByNombreEstadoAndEliminadoFalse(
            String nombreEstado);

    boolean existsByNombreEstadoAndIdEstadoPedidoNotAndEliminadoFalse(
            String nombreEstado,
            Integer idEstadoPedido);

}
