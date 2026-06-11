
package com.utp.RestoControl.Entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalles_pedidos")

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor

@Builder
public class DetallePedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;
    
    @Column(name = "cantidad")
    private Integer cantidad;
    
    @Column(nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal precio_unitario;
    
    @Column(nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal subtotal;
    
    @Column(nullable = false)
    private Boolean eliminado = false;
    
    @ManyToOne
    @JoinColumn(name = "id_alimento",
                nullable = false)
     private Alimento idAlimento;
    
    @ManyToOne
    @JoinColumn(name = "id_pedido",
                nullable = false)
     private Pedido idPedido;
    
}
