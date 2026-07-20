package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auditoria_operaciones", indexes = {
    @Index(name = "idx_auditoria_fecha", columnList = "fecha_hora"),
    @Index(name = "idx_auditoria_usuario_fecha", columnList = "id_usuario, fecha_hora"),
    @Index(name = "idx_auditoria_modulo_fecha", columnList = "modulo, fecha_hora"),
    @Index(name = "idx_auditoria_resultado_fecha", columnList = "resultado, fecha_hora")
})
public class AuditoriaOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "correo_usuario", nullable = false, length = 120)
    private String correoUsuario;

    @Column(name = "nombre_usuario", nullable = false, length = 170)
    private String nombreUsuario;

    @Column(name = "rol_usuario", nullable = false, length = 40)
    private String rolUsuario;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 70)
    private String accion;

    @Column(name = "metodo_http", nullable = false, length = 10)
    private String metodoHttp;

    @Column(nullable = false, length = 255)
    private String ruta;

    @Column(name = "recurso_id", length = 80)
    private String recursoId;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(name = "estado_http", nullable = false)
    private Integer estadoHttp;

    @Column(name = "duracion_ms", nullable = false)
    private Long duracionMs;

    @Column(name = "direccion_ip", length = 64)
    private String direccionIp;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(length = 500)
    private String detalle;

    @Column(name = "tipo_error", length = 120)
    private String tipoError;
}
