package com.utp.RestoControl.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "alimentos", indexes = {
    @Index(name = "idx_alimentos_categoria_eliminado", columnList = "id_categoria, eliminado"),
    @Index(name = "idx_alimentos_eliminado_disponible", columnList = "eliminado, disponible"),
    @Index(name = "idx_alimentos_bloqueo_cocina", columnList = "bloqueado_cocina, eliminado")
})

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor

@Builder
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id_alimento")
    private Integer idAlimento;

    @Column(name = "nombre_alimento",
            nullable = false,
            length = 100)
    private String nombreAlimento;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean disponible = true;

    @Column(name = "bloqueado_cocina", nullable = false)
    private Boolean bloqueadoCocina = false;

    @Column(name = "motivo_bloqueo_cocina", length = 200)
    private String motivoBloqueoCocina;

    @Column(name = "fecha_bloqueo_cocina")
    private LocalDateTime fechaBloqueoCocina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_bloqueo_cocina")
    private Usuario usuarioBloqueoCocina;

    @Column(nullable = false)
    private Boolean eliminado = false;

    /*
        RELACIÓN:
        Muchos alimentos pertenecen a una categoría
     */

    @ManyToOne
    @JoinColumn(name = "id_categoria",
                nullable = false)

    private CategoriaAlimento categoria;

    @JsonIgnore
    @OneToMany(mappedBy = "alimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecetaAlimento> receta = new ArrayList<>();
}
