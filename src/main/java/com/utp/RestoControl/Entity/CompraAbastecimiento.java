package com.utp.RestoControl.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "compras_abastecimiento",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_compra_proveedor_documento",
                columnNames = {"id_proveedor", "numero_documento"}),
        indexes = {
            @Index(name = "idx_compras_fecha", columnList = "fecha_compra"),
            @Index(name = "idx_compras_usuario", columnList = "id_usuario_almacenero")
        })
public class CompraAbastecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Integer idCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_almacenero", nullable = false)
    private Usuario usuarioAlmacenero;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "numero_documento", nullable = false, length = 60)
    private String numeroDocumento;

    @Column(length = 250)
    private String observacion;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    private List<CompraAbastecimientoDetalle> detalles = new ArrayList<>();
}
