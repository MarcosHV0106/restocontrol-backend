package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Pedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository
        extends JpaRepository<Pedido, Integer> {

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.idMesa m
            join fetch m.estadoMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            order by p.idPedido desc
            """)
    List<Pedido> findActivosConRelaciones();

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.idMesa m
            join fetch m.estadoMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and u.idUsuario = :idUsuario
            order by p.idPedido desc
            """)
    List<Pedido> findActivosConRelacionesByUsuario(@Param("idUsuario") Integer idUsuario);

    @EntityGraph(attributePaths = {
            "idMesa.estadoMesa",
            "usuario.rol",
            "estadoPedido",
            "modalidadPedido",
            "detalles.idAlimento.categoria"
    })
    List<Pedido> findByUsuario_IdUsuario(Integer idUsuario);

    List<Pedido> findByEliminadoFalse();

    @EntityGraph(attributePaths = {
            "idMesa.estadoMesa",
            "usuario.rol",
            "estadoPedido",
            "modalidadPedido",
            "detalles.idAlimento.categoria"
    })
    Optional<Pedido> findByIdPedidoAndEliminadoFalse(
            Integer idPedido);

    @EntityGraph(attributePaths = {
            "idMesa.estadoMesa",
            "usuario.rol",
            "estadoPedido",
            "modalidadPedido",
            "detalles.idAlimento.categoria"
    })
    Optional<Pedido> findTopByIdMesa_IdMesaAndEliminadoFalseOrderByIdPedidoDesc(
            Integer idMesa
    );

    @EntityGraph(attributePaths = {
            "idMesa.estadoMesa",
            "usuario.rol",
            "estadoPedido",
            "modalidadPedido",
            "detalles.idAlimento.categoria"
    })
    Optional<Pedido> findTopByIdMesa_IdMesaAndEstadoPedido_IdEstadoPedidoNotAndEliminadoFalseOrderByIdPedidoDesc(
        Integer idMesa,
        Integer idEstadoPedido
    );

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.idMesa m
            join fetch p.estadoPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and m.idMesa in :idsMesa
            and p.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
            and p.idPedido in (
                select max(p2.idPedido)
                from Pedido p2
                where p2.eliminado = false
                and p2.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
                and p2.idMesa.idMesa in :idsMesa
                group by p2.idMesa.idMesa
            )
            """)
    List<Pedido> findUltimosActivosPorMesas(
            @Param("idsMesa") List<Integer> idsMesa,
            @Param("idEstadoPedidoExcluido") Integer idEstadoPedidoExcluido
    );
}
