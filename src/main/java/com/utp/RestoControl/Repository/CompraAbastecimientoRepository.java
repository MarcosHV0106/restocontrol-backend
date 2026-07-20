package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.CompraAbastecimiento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraAbastecimientoRepository extends JpaRepository<CompraAbastecimiento, Integer> {

    @EntityGraph(attributePaths = {"proveedor", "usuarioAlmacenero", "detalles", "detalles.insumo", "detalles.lote"})
    List<CompraAbastecimiento> findByEliminadoFalseOrderByFechaCompraDescIdCompraDesc();

    @EntityGraph(attributePaths = {"proveedor", "usuarioAlmacenero", "detalles", "detalles.insumo", "detalles.lote"})
    Optional<CompraAbastecimiento> findByIdCompraAndEliminadoFalse(Integer idCompra);

    @Query("""
            select c from CompraAbastecimiento c
            where c.eliminado = false
              and c.fechaCompra >= :desde
              and c.fechaCompra <= :hasta
            order by c.fechaCompra desc, c.idCompra desc
            """)
    List<CompraAbastecimiento> findParaReporte(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    boolean existsByProveedor_IdProveedorAndNumeroDocumentoIgnoreCaseAndEliminadoFalse(
            Integer idProveedor, String numeroDocumento);
}
