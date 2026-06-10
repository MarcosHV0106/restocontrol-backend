
package com.utp.RestoControl.Entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "mesas")
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
    
    @Column(name = "estado_mesa", nullable = false, length = 30)
    @JsonAlias("estado_mesa")
    private String estadoMesa;

    @Column(nullable = false)
    private Boolean eliminado = false;
    
}
