package com.utp.RestoControl.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos", indexes = {
    @Index(name = "idx_pedidos_eliminado", columnList = "eliminado"),
    @Index(name = "idx_pedidos_mesa_estado_eliminado", columnList = "id_mesa, id_estado_pedido, eliminado"),
    @Index(name = "idx_pedidos_usuario_eliminado", columnList = "id_usuario, eliminado"),
    @Index(name = "idx_pedidos_mesa_id", columnList = "id_mesa, id_pedido"),
    @Index(name = "idx_pedidos_consumo_eliminado", columnList = "fecha_consumo_inventario, eliminado"),
    @Index(name = "idx_pedidos_cancelacion_eliminado", columnList = "fecha_cancelacion, eliminado")
})

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor

@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(nullable = false, name = "fecha")
    private LocalDateTime fechaPedido;

    @Column(nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal total;

    @Column(name = "observacion", length = 250)
    private String observacion;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_inicio_preparacion")
    private LocalDateTime fechaInicioPreparacion;

    @Column(name = "fecha_listo")
    private LocalDateTime fechaListo;

    @Column(name = "fecha_entregado")
    private LocalDateTime fechaEntregado;

    @Column(name = "fecha_consumo_inventario")
    private LocalDateTime fechaConsumoInventario;

    @Column(name = "fecha_envio_cocina")
    private LocalDateTime fechaEnvioCocina;

    @Column(name = "fecha_solicitud_cuenta")
    private LocalDateTime fechaSolicitudCuenta;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Column(name = "cliente_nombre", length = 120)
    private String clienteNombre;

    @Column(name = "cliente_telefono", length = 20)
    private String clienteTelefono;

    @Column(name = "direccion_entrega", length = 250)
    private String direccionEntrega;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @ManyToOne
    @JoinColumn(name = "id_modalidad_pedido",
            nullable = false)
    private ModalidadPedido modalidadPedido;

    @ManyToOne
    @JoinColumn(name = "id_mesa",
            nullable = true)
    private Mesa idMesa;

    @ManyToOne
    @JoinColumn(name = "id_estado_pedido",
            nullable = false)
    private EstadoPedido estadoPedido;

    @OneToMany(
            mappedBy = "idPedido",
            cascade = CascadeType.ALL
    )
    private List<DetallePedido> detalles;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false) // Esta es la única referencia necesaria
    private Usuario usuario;
}
