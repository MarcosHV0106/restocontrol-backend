package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.DetallePedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Integer> {

    List<DetallePedido> findByIdPedido_IdPedidoAndEliminadoFalse(
            Integer idPedido);

}
