
package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movimientos_inventarios")
public class MovimientoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;
     
    @Column(name = "tipo_movimiento",length = 30)
    private String tipoMovimiento;
    
    @Column(name = "cantidad")
    private Double cantidad;
    
    @Column(name = "motivo",length = 150)
    private String motivo;
    
    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;
    
    @Column(name = "referencia",length = 100)
    private String referencia;
    
   @ManyToOne
   @JoinColumn(name = "id_insumo",
                nullable = false)
    private Insumo insumo;
    
    
    
}
