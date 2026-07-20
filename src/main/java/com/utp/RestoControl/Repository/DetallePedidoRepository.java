package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.DetallePedido;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Integer> {

    List<DetallePedido> findByIdPedido_IdPedidoAndEliminadoFalse(
            Integer idPedido);

    @Query("""
            select d.idAlimento.idAlimento as idAlimento,
                   sum(d.cantidad) as cantidad
            from DetallePedido d
            join d.idPedido p
            where d.eliminado = false
              and p.eliminado = false
              and p.fechaConsumoInventario >= :desde
              and p.fechaConsumoInventario < :hastaExclusiva
            group by d.idAlimento.idAlimento
            """)
    List<CantidadProcesadaPorAlimento> sumarCantidadesProcesadas(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusiva") LocalDateTime hastaExclusiva
    );

    interface CantidadProcesadaPorAlimento {
        Integer getIdAlimento();

        Long getCantidad();
    }

}
