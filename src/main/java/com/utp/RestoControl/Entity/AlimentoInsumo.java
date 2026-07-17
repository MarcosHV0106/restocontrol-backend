
package com.utp.RestoControl.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "alimento_insumo",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_alimento_insumo",
                    columnNames = {"id_alimento", "id_insumo"}
            )
        })
public class AlimentoInsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alimento_insumo")
    private Integer idAlimentoInsumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alimento", nullable = false)
    private Alimento alimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    @Column(name = "cantidad_referencial")
    private Double cantidadReferencial;

    @Column(name = "unidad_medida", length = 20)
    private String unidadMedida;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Column(nullable = false)
    private Boolean eliminado = false;
}
