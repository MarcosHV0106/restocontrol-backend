package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.AlertaInventario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaInventarioRepository extends JpaRepository<AlertaInventario, Integer> {

    @EntityGraph(attributePaths = {"insumo", "lote", "usuarioAtencion"})
    List<AlertaInventario> findByEliminadoFalseOrderByFechaGeneracionDesc();

    @EntityGraph(attributePaths = {"insumo", "lote", "usuarioAtencion"})
    Optional<AlertaInventario> findByIdAlertaAndEliminadoFalse(Integer idAlerta);

    Optional<AlertaInventario> findByClaveActivaAndEliminadoFalse(String claveActiva);

    long countByEstadoAndEliminadoFalse(String estado);
}
