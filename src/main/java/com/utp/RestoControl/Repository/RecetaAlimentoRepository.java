package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.RecetaAlimento;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecetaAlimentoRepository extends JpaRepository<RecetaAlimento, Integer> {

    @EntityGraph(attributePaths = "insumo")
    List<RecetaAlimento> findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(Integer idAlimento);

    void deleteByAlimento_IdAlimento(Integer idAlimento);
}
