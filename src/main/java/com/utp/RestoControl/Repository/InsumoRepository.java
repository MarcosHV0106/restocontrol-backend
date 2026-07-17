
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Insumo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface InsumoRepository extends JpaRepository<Insumo, Integer>{
    List<Insumo> findByEliminadoFalse();
    Optional<Insumo> findByIdInsumoAndEliminadoFalse(Integer idInsumo);
}
