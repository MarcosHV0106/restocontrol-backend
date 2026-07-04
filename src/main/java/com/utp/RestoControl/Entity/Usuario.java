
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;
    
    @Column(name = "nombres", nullable = false, length = 80)
    private String nombre;
    
    @Column(name = "apellidos", nullable = false, length = 80)
    private String apellido;
    
    @Column(name = "correo", nullable = false, length = 100)
    private String correo;
    
    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "pendiente", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean pendiente = false;

    @Column(name = "token_activacion_hash", length = 120)
    private String tokenActivacionHash;

    @Column(name = "token_activacion_expira")
    private LocalDateTime tokenActivacionExpira;
    
    @ManyToOne
    @JoinColumn(name = "id_rol",
                nullable = false)

    private Rol rol;

    @Column(nullable = false)
    private Boolean disponible = true;

    @Column(nullable = false)
    private Boolean eliminado = false;
    
    
}
