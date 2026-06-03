package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.CategoriaAlimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaAlimento, Integer> {

    List<CategoriaAlimento> findByEliminadoFalse();

    Optional<CategoriaAlimento> findByIdCategoriaAndEliminadoFalse(Integer idCategoria);

    boolean existsByNombreCategoriaIgnoreCaseAndEliminadoFalse(String nombreCategoria);

    boolean existsByNombreCategoriaIgnoreCaseAndIdCategoriaNotAndEliminadoFalse(String nombreCategoria, Integer idCategoria);
}
