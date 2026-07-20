package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Proveedor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    List<Proveedor> findByEliminadoFalseOrderByRazonSocialAsc();
    Optional<Proveedor> findByIdProveedorAndEliminadoFalse(Integer idProveedor);
    boolean existsByRuc(String ruc);
    boolean existsByRucAndIdProveedorNot(String ruc, Integer idProveedor);
}
