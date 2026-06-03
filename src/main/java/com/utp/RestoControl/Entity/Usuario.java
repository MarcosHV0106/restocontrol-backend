
package com.utp.RestoControl.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    
    @ManyToOne
    @JoinColumn(name = "id_rol",
                nullable = false)

    private Rol rol;

    @Column(nullable = false)
    private Boolean eliminado = false;
    
    
}
