package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.EstimacionDiaria;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstimacionDiariaRepository extends JpaRepository<EstimacionDiaria, Integer> {

    @EntityGraph(attributePaths = {"alimento.categoria", "alimento.receta.insumo", "usuario"})
    List<EstimacionDiaria> findByFechaAndEliminadoFalse(LocalDate fecha);

    @EntityGraph(attributePaths = {"alimento.categoria", "alimento.receta.insumo", "usuario"})
    List<EstimacionDiaria> findByFecha(LocalDate fecha);
}
