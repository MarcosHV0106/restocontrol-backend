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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cobros",
        uniqueConstraints = @UniqueConstraint(name = "uq_cobros_pedido", columnNames = "id_pedido"),
        indexes = {
            @Index(name = "idx_cobros_fecha", columnList = "fecha_cobro"),
            @Index(name = "idx_cobros_cajero", columnList = "id_usuario_cajero"),
            @Index(name = "idx_cobros_eliminado", columnList = "eliminado")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cobro")
    private Integer idCobro;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "fecha_cobro", nullable = false)
    private LocalDateTime fechaCobro;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "total_cobrado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCobrado;

    @Column(name = "total_recibido", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRecibido;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vuelto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cajero", nullable = false)
    private Usuario usuarioCajero;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @OneToMany(mappedBy = "cobro", cascade = CascadeType.ALL)
    private List<PagoCobro> pagos = new ArrayList<>();
}
