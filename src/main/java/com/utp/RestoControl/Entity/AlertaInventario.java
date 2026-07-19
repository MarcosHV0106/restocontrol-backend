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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "alertas_inventario")
public class AlertaInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false, length = 250)
    private String detalle;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    @ManyToOne
    @JoinColumn(name = "id_lote")
    private LoteInsumo lote;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(length = 40)
    private String accion;

    @Column(length = 250)
    private String observacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario_atencion")
    private Usuario usuarioAtencion;

    @Column(name = "clave_activa", unique = true, length = 100)
    private String claveActiva;

    @Column(nullable = false)
    private Boolean eliminado = false;
}
