package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.LoteInsumo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteInsumoRepository extends JpaRepository<LoteInsumo, Integer> {

    @EntityGraph(attributePaths = "insumo")
    List<LoteInsumo> findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(Integer idInsumo);

    @EntityGraph(attributePaths = "insumo")
    List<LoteInsumo> findByEliminadoFalse();

    @EntityGraph(attributePaths = "insumo")
    Optional<LoteInsumo> findByIdLoteAndEliminadoFalse(Integer idLote);

    boolean existsByCodigoIgnoreCase(String codigo);
}
