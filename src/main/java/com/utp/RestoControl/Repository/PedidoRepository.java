package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Pedido;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PedidoRepository
        extends JpaRepository<Pedido, Integer> {

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa m
            left join fetch m.estadoMesa
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
            left join fetch p.idMesa m
            left join fetch m.estadoMesa
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

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa m
            left join fetch m.estadoMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido ep
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and p.fechaSolicitudCuenta is not null
            and ep.idEstadoPedido <> 4
            and upper(ep.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
            and not exists (
                select c.idCobro from Cobro c
                where c.pedido.idPedido = p.idPedido and c.eliminado = false
            )
            order by p.fechaPedido asc
            """)
    List<Pedido> findPendientesDeCobroConRelaciones();

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa m
            left join fetch m.estadoMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido ep
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and u.idUsuario = :idUsuario
            and p.fechaSolicitudCuenta is not null
            and ep.idEstadoPedido <> 4
            and upper(ep.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
            and not exists (
                select c.idCobro from Cobro c
                where c.pedido.idPedido = p.idPedido and c.eliminado = false
            )
            order by p.fechaPedido asc
            """)
    List<Pedido> findPendientesDeCobroPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa m
            left join fetch m.estadoMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido ep
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and p.fechaEnvioCocina is not null
            and (
                upper(ep.nombreEstado) in (
                    'PENDIENTE', 'RECIBIDO', 'EN PREPARACION',
                    'EN PREPARACIÓN', 'PREPARANDO', 'LISTO'
                )
                or (
                    upper(ep.nombreEstado) = 'ENTREGADO'
                    and p.fechaEntregado >= :desdeEntregados
                )
            )
            order by p.fechaPedido asc
            """)
    List<Pedido> findParaCocina(@Param("desdeEntregados") LocalDateTime desdeEntregados);

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and p.fechaEntregado >= :desde
            and p.fechaEntregado < :hasta
            order by p.fechaEntregado desc
            """)
    List<Pedido> findHistorialCocina(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @EntityGraph(attributePaths = {"detalles"})
    @Query("""
            select distinct p from Pedido p
            where p.eliminado = false
            and p.fechaInicioPreparacion is not null
            and p.fechaListo is not null
            and p.fechaListo >= :desde
            """)
    List<Pedido> findPreparadosParaEstimacion(@Param("desde") LocalDateTime desde);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "idMesa.estadoMesa",
        "usuario.rol",
        "estadoPedido",
        "modalidadPedido",
        "detalles.idAlimento.categoria"
    })
    @Query("""
            select p from Pedido p
            where p.idPedido = :idPedido
            and p.eliminado = false
            """)
    Optional<Pedido> findActivoParaCocina(@Param("idPedido") Integer idPedido);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "idMesa.estadoMesa",
        "usuario.rol",
        "estadoPedido",
        "modalidadPedido",
        "detalles.idAlimento.categoria"
    })
    @Query("""
            select p from Pedido p
            where p.idPedido = :idPedido
            and p.eliminado = false
            """)
    Optional<Pedido> findActivoParaGestion(@Param("idPedido") Integer idPedido);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "idMesa.estadoMesa",
        "usuario.rol",
        "estadoPedido",
        "modalidadPedido",
        "detalles.idAlimento.categoria"
    })
    @Query("""
            select p from Pedido p
            where p.idPedido = :idPedido
            and p.eliminado = false
            """)
    Optional<Pedido> findActivoParaCobro(@Param("idPedido") Integer idPedido);

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
    @Query("""
            select p from Pedido p
            join p.estadoPedido ep
            where p.idMesa.idMesa = :idMesa
            and p.eliminado = false
            and ep.idEstadoPedido <> 4
            and upper(ep.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
            and not exists (
                select c.idCobro from Cobro c
                where c.pedido.idPedido = p.idPedido and c.eliminado = false
            )
            order by p.idPedido desc
            """)
    List<Pedido> findActivosPorMesa(@Param("idMesa") Integer idMesa);

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.idMesa m
            join fetch p.estadoPedido ep
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and m.idMesa in :idsMesa
            and p.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
            and upper(ep.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
            and p.idPedido in (
                select max(p2.idPedido)
                from Pedido p2
                where p2.eliminado = false
                and p2.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
                and upper(p2.estadoPedido.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
                and p2.idMesa.idMesa in :idsMesa
                group by p2.idMesa.idMesa
            )
            """)
    List<Pedido> findUltimosActivosPorMesas(
            @Param("idsMesa") List<Integer> idsMesa,
            @Param("idEstadoPedidoExcluido") Integer idEstadoPedidoExcluido
    );

    @Query("""
            select distinct p
            from Pedido p
            join fetch p.idMesa m
            join fetch p.usuario u
            join fetch p.estadoPedido ep
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and m.idMesa in :idsMesa
            and p.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
            and upper(ep.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
            and u.idUsuario = :idUsuario
            and p.idPedido in (
                select max(p2.idPedido)
                from Pedido p2
                where p2.eliminado = false
                and p2.estadoPedido.idEstadoPedido <> :idEstadoPedidoExcluido
                and upper(p2.estadoPedido.nombreEstado) not in ('PAGADO', 'COBRADO', 'CANCELADO')
                and p2.idMesa.idMesa in :idsMesa
                group by p2.idMesa.idMesa
            )
            """)
    List<Pedido> findUltimosActivosPorMesasDelUsuario(
            @Param("idsMesa") List<Integer> idsMesa,
            @Param("idEstadoPedidoExcluido") Integer idEstadoPedidoExcluido,
            @Param("idUsuario") Integer idUsuario
    );

    @Query("""
            select distinct p
            from Pedido p
            left join fetch p.idMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido ep
            join fetch p.modalidadPedido
            left join fetch p.detalles d
            left join fetch d.idAlimento a
            left join fetch a.categoria
            where p.eliminado = false
            and (upper(ep.nombreEstado) in ('PAGADO', 'COBRADO') or ep.idEstadoPedido = 4)
            and coalesce(p.fechaPago, p.fechaPedido) >= :desde
            and coalesce(p.fechaPago, p.fechaPedido) < :hastaExclusiva
            order by p.fechaPedido asc
            """)
    List<Pedido> findVentasParaReporte(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusiva") LocalDateTime hastaExclusiva
    );

    @Query("""
            select p
            from Pedido p
            left join fetch p.idMesa
            join fetch p.usuario u
            join fetch u.rol
            join fetch p.estadoPedido ep
            join fetch p.modalidadPedido
            where p.eliminado = false
            and upper(ep.nombreEstado) = 'CANCELADO'
            and coalesce(p.fechaCancelacion, p.fechaPedido) >= :desde
            and coalesce(p.fechaCancelacion, p.fechaPedido) < :hastaExclusiva
            order by coalesce(p.fechaCancelacion, p.fechaPedido) desc
            """)
    List<Pedido> findCanceladosParaReporte(
            @Param("desde") LocalDateTime desde,
            @Param("hastaExclusiva") LocalDateTime hastaExclusiva
    );
}
