package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "proveedores",
        uniqueConstraints = @UniqueConstraint(name = "uq_proveedores_ruc", columnNames = "ruc"),
        indexes = @Index(name = "idx_proveedores_activo", columnList = "activo, eliminado"))
public class Proveedor {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(length = 120)
    private String contacto;

    @Column(length = 20)
    private String telefono;

    @Column(length = 120)
    private String correo;

    @Column(length = 250)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void prepararCreacion() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        fechaCreacion = fechaCreacion == null ? ahora : fechaCreacion;
        fechaActualizacion = ahora;
        activo = activo == null || activo;
        eliminado = Boolean.TRUE.equals(eliminado);
    }

    @PreUpdate
    void prepararActualizacion() {
        fechaActualizacion = LocalDateTime.now(ZONA_LIMA);
    }
}
