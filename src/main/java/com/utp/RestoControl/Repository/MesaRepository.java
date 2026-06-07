
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Mesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MesaRepository extends JpaRepository<Mesa, Integer>{

    List<Mesa> findByEliminadoFalse();

    Optional<Mesa> findByIdMesaAndEliminadoFalse(Integer idMesa);

    boolean existsByNumeroMesaAndEliminadoFalse(Integer numeroMesa);

    boolean existsByNumeroMesaAndIdMesaNotAndEliminadoFalse(Integer numeroMesa, Integer idMesa);
    
    Integer countByEstadoMesaAndEliminadoFalse(String estadoMesa);

}
