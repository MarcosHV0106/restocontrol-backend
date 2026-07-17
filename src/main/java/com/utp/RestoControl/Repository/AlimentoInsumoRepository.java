
package com.utp.RestoControl.Repository;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.AlimentoInsumo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlimentoInsumoRepository extends JpaRepository<AlimentoInsumo, Integer> {

    boolean existsByAlimentoIdAlimentoAndInsumoIdInsumo(
            Integer idAlimento,
            Integer idInsumo);

    List<AlimentoInsumo> findByAlimentoIdAlimentoAndEliminadoFalse(Integer idAlimento);

    List<AlimentoInsumo> findByInsumoIdInsumoAndEliminadoFalse(Integer idInsumo);
    
    List<AlimentoInsumo> findByEliminadoFalse();
    
    Optional<AlimentoInsumo> findByIdAlimentoInsumoAndEliminadoFalse(Integer idAlimentoInsumo);
    

}
