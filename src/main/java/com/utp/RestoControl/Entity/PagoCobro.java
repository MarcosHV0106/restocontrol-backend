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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "pagos_cobro",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pagos_cobro_comprobante",
                columnNames = "numero_comprobante"
        ),
        indexes = {
            @Index(name = "idx_pagos_cobro_cobro", columnList = "id_cobro"),
            @Index(name = "idx_pagos_cobro_metodo", columnList = "metodo_pago"),
            @Index(name = "idx_pagos_cobro_eliminado", columnList = "eliminado")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_cobro")
    private Integer idPagoCobro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cobro", nullable = false)
    private Cobro cobro;

    @Column(nullable = false)
    private Integer secuencia;

    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_recibido", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoRecibido;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal vuelto;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private String tipoComprobante;

    @Column(name = "numero_comprobante", length = 30)
    private String numeroComprobante;

    @Column(name = "documento_cliente", length = 20)
    private String documentoCliente;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(length = 80)
    private String referencia;

    @Column(nullable = false)
    private Boolean eliminado = false;
}
