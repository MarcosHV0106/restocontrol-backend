
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Insumo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface InsumoRepository extends JpaRepository<Insumo, Integer>{
    Optional<Insumo> findByIdInsumo(Integer idInsumo);
}
