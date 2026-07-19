
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Mesa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;


public interface MesaRepository extends JpaRepository<Mesa, Integer>{
 
    @EntityGraph(attributePaths = "estadoMesa")
    List<Mesa> findByEliminadoFalse();

    @EntityGraph(attributePaths = "estadoMesa")
    List<Mesa> findByEliminadoFalseOrderByNumeroMesaAsc();

    @EntityGraph(attributePaths = "estadoMesa")
    Optional<Mesa> findByIdMesaAndEliminadoFalse(Integer idMesa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "estadoMesa")
    @Query("""
            select m from Mesa m
            where m.idMesa in :idsMesa
            and m.eliminado = false
            order by m.idMesa
            """)
    List<Mesa> findActivasParaActualizar(@Param("idsMesa") List<Integer> idsMesa);

    boolean existsByNumeroMesaAndEliminadoFalse(Integer numeroMesa);

    boolean existsByNumeroMesaAndIdMesaNotAndEliminadoFalse(Integer numeroMesa, Integer idMesa);
    
    Integer countByEstadoMesa_IdEstadoMesaAndEliminadoFalse(Integer estadoMesa);

    @Query("""
            select e.descripcion, count(m)
            from Mesa m
            join m.estadoMesa e
            where m.eliminado = false
            group by e.descripcion
            """)
    List<Object[]> countMesasAgrupadasPorEstado();
    
}
