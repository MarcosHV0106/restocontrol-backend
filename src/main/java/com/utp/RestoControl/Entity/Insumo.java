
package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "insumos")
public class Insumo {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Integer idInsumo;
     
    @Column(name = "nombre_insumo",length = 100)
    private String nombreInsumo;
    
    @Column(name = "unidad_medida",length = 30)
    private String unidadMedida;
    
    @Column(name = "stock_actual")
    private Double stockActual;
    
    @Column(name = "stock_minimo")
    private Double stockMinimo;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4,
            columnDefinition = "DECIMAL(12,4) DEFAULT 0.0000")
    private BigDecimal costoUnitario = BigDecimal.ZERO;
    
    
    
}
