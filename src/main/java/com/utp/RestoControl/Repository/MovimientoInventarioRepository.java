
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.MovimientoInventario;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer>{
    @EntityGraph(attributePaths = {"insumo", "lote", "usuario", "pedido"})
    List<MovimientoInventario> findByEliminadoFalseOrderByFechaMovimientoDesc();

    @EntityGraph(attributePaths = {"insumo", "lote", "usuario"})
    @Query("""
            select m from MovimientoInventario m
            where m.eliminado = false
              and m.fechaMovimiento >= :desde
              and m.fechaMovimiento < :hastaExclusiva
            order by m.fechaMovimiento desc, m.idMovimiento desc
            """)
    List<MovimientoInventario> findParaReporte(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusiva") LocalDateTime hastaExclusiva
    );
}
