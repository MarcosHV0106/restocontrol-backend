/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Alimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlimentoRepository
        extends JpaRepository<Alimento, Integer> {

    @EntityGraph(attributePaths = "categoria")
    List<Alimento> findByEliminadoFalse();

    @EntityGraph(attributePaths = "categoria")
    List<Alimento> findByCategoria_IdCategoriaAndEliminadoFalse(Integer idCategoria);

    @EntityGraph(attributePaths = "categoria")
    Optional<Alimento> findByIdAlimentoAndEliminadoFalse(Integer idAlimento);
}
