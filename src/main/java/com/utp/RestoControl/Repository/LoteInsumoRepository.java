package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.LoteInsumo;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteInsumoRepository extends JpaRepository<LoteInsumo, Integer> {

    @EntityGraph(attributePaths = "insumo")
    List<LoteInsumo> findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(Integer idInsumo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from LoteInsumo l
            join fetch l.insumo
            where l.insumo.idInsumo = :idInsumo
            and l.eliminado = false
            and upper(l.estado) = 'ACTIVO'
            and l.cantidadActual > 0
            and (l.fechaVencimiento is null or l.fechaVencimiento >= :hoy)
            order by case when l.fechaVencimiento is null then 1 else 0 end,
                     l.fechaVencimiento asc,
                     l.fechaIngreso asc,
                     l.idLote asc
            """)
    List<LoteInsumo> findConsumiblesParaPedido(
            @Param("idInsumo") Integer idInsumo,
            @Param("hoy") LocalDate hoy
    );

    @EntityGraph(attributePaths = "insumo")
    List<LoteInsumo> findByEliminadoFalse();

    @EntityGraph(attributePaths = "insumo")
    Optional<LoteInsumo> findByIdLoteAndEliminadoFalse(Integer idLote);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteInsumo l join fetch l.insumo where l.idLote = :idLote and l.eliminado = false")
    Optional<LoteInsumo> findParaActualizar(@Param("idLote") Integer idLote);

    boolean existsByCodigoIgnoreCase(String codigo);
}
