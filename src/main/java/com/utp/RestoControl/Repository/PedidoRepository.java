package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Pedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository
        extends JpaRepository<Pedido, Integer> {

    // PedidoRepository.java
    List<Pedido> findByUsuario_IdUsuario(Integer idUsuario);

    List<Pedido> findByEliminadoFalse();

    Optional<Pedido> findByIdPedidoAndEliminadoFalse(
            Integer idPedido);

    Optional<Pedido> findTopByIdMesa_IdMesaAndEliminadoFalseOrderByIdPedidoDesc(
            Integer idMesa
    );
    Optional<Pedido> findTopByIdMesa_IdMesaAndEstadoPedido_IdEstadoPedidoNotOrderByIdPedidoDesc(
        Integer idMesa,
        Integer idEstadoPedido
    );

}
