package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "estimaciones_diarias",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estimacion_fecha_alimento",
                columnNames = {"fecha", "id_alimento"}),
        indexes = {
            @Index(name = "idx_estimacion_fecha_eliminado", columnList = "fecha, eliminado"),
            @Index(name = "idx_estimacion_alimento", columnList = "id_alimento")
        })
public class EstimacionDiaria {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estimacion_diaria")
    private Integer idEstimacionDiaria;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_alimento", nullable = false)
    private Alimento alimento;

    @Column(nullable = false)
    private Integer porciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @PrePersist
    void prepararCreacion() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        fechaCreacion = fechaCreacion == null ? ahora : fechaCreacion;
        fechaActualizacion = ahora;
        eliminado = Boolean.TRUE.equals(eliminado);
    }

    @PreUpdate
    void prepararActualizacion() {
        fechaActualizacion = LocalDateTime.now(ZONA_LIMA);
    }
}
