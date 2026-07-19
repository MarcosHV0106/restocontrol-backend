
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.MovimientoInventario;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer>{
    @EntityGraph(attributePaths = {"insumo", "lote", "usuario", "pedido"})
    List<MovimientoInventario> findByEliminadoFalseOrderByFechaMovimientoDesc();
}
