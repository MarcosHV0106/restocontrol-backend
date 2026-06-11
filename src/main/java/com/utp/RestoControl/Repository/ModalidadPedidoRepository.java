package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.ModalidadPedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModalidadPedidoRepository
        extends JpaRepository<ModalidadPedido, Integer> {

    List<ModalidadPedido> findByEliminadoFalse();

    Optional<ModalidadPedido> findByIdModalidadPedidoAndEliminadoFalse(
            Integer idModalidaPedido);

}
