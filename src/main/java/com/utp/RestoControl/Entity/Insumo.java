
package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "nombre_insumo", length = 100, nullable = false)
    private String nombreInsumo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "unidad_medida", length = 30, nullable = false)
    private String unidadMedida;

    @Column(name = "stock_actual", precision = 12, scale = 4)
    private BigDecimal stockActual;

    @Column(name = "stock_minimo", precision = 12, scale = 4)
    private BigDecimal stockMinimo;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "eliminado")
    private Boolean eliminado = false;

}
