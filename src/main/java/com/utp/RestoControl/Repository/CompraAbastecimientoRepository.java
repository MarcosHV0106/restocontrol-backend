package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.CompraAbastecimiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraAbastecimientoRepository extends JpaRepository<CompraAbastecimiento, Integer> {

    @EntityGraph(attributePaths = {"proveedor", "usuarioAlmacenero", "detalles", "detalles.insumo", "detalles.lote"})
    List<CompraAbastecimiento> findByEliminadoFalseOrderByFechaCompraDescIdCompraDesc();

    @EntityGraph(attributePaths = {"proveedor", "usuarioAlmacenero", "detalles", "detalles.insumo", "detalles.lote"})
    Optional<CompraAbastecimiento> findByIdCompraAndEliminadoFalse(Integer idCompra);

    boolean existsByProveedor_IdProveedorAndNumeroDocumentoIgnoreCaseAndEliminadoFalse(
            Integer idProveedor, String numeroDocumento);
}
