
package com.utp.RestoControl.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")

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
    
    @Column(nullable = false)
    private Boolean eliminado = false;
    
    
    
    @ManyToOne
    @JoinColumn(name = "id_modalidad_pedido",
                nullable = false)
     private ModalidadPedido modalidadPedido;
    
    @ManyToOne
    @JoinColumn(name = "id_mesa",
                nullable = false)
     private Mesa idMesa;
    
    @ManyToOne
    @JoinColumn(name = "id_usuario",
                nullable = false)
     private Usuario idUsuario;
    
    @ManyToOne
    @JoinColumn(name = "id_estado_pedido",
                nullable = false)
     private EstadoPedido estadoPedido;

}
