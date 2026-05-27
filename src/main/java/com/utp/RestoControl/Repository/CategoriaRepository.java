package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.CategoriaAlimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<CategoriaAlimento, Integer> {
}