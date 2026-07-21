/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Alimento;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlimentoRepository
        extends JpaRepository<Alimento, Integer> {

    @EntityGraph(attributePaths = {"categoria", "usuarioBloqueoCocina"})
    List<Alimento> findByEliminadoFalse();

    @EntityGraph(attributePaths = {"categoria", "usuarioBloqueoCocina"})
    List<Alimento> findByCategoria_IdCategoriaAndEliminadoFalse(Integer idCategoria);

    @EntityGraph(attributePaths = {"categoria", "usuarioBloqueoCocina"})
    Optional<Alimento> findByIdAlimentoAndEliminadoFalse(Integer idAlimento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"categoria", "usuarioBloqueoCocina"})
    @Query("""
            select a from Alimento a
            where a.idAlimento = :idAlimento
            and a.eliminado = false
            """)
    Optional<Alimento> findActivoParaCocina(@Param("idAlimento") Integer idAlimento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from Alimento a
            where a.idAlimento = :idAlimento
            and a.eliminado = false
            """)
    Optional<Alimento> findActivoParaActualizarStock(@Param("idAlimento") Integer idAlimento);
}
