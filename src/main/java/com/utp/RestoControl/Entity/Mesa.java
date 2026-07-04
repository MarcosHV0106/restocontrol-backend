
package com.utp.RestoControl.Entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mesas", indexes = {
    @Index(name = "idx_mesas_eliminado", columnList = "eliminado"),
    @Index(name = "idx_mesas_estado_eliminado", columnList = "id_estado_mesa, eliminado"),
    @Index(name = "idx_mesas_numero_eliminado", columnList = "numero_mesa, eliminado")
})
public class Mesa {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mesa")
    private Integer idMesa;
     
    @Column(name = "numero_mesa", nullable = false)
    private Integer numeroMesa;
    
    @Column(name = "capacidad", nullable = false)
    private Integer capacidad;
    
    @Column(name = "piso", nullable = false)
    private Integer piso;
    
    @ManyToOne
    @JoinColumn(name = "id_estado_mesa",
            nullable = false)
    private EstadoMesa estadoMesa;

    @Column(nullable = false)
    private Boolean eliminado = false;
    
}
