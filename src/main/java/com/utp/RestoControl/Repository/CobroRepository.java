package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Cobro;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CobroRepository extends JpaRepository<Cobro, Integer> {

    boolean existsByPedido_IdPedidoAndEliminadoFalse(Integer idPedido);

    @EntityGraph(attributePaths = {
        "pedido.idMesa",
        "pedido.usuario.rol",
        "usuarioCajero.rol",
        "pagos"
    })
    Optional<Cobro> findByIdCobroAndEliminadoFalse(Integer idCobro);
}
