
package com.utp.RestoControl.Repository;

import com.utp.RestoControl.Entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UsuarioRepository extends JpaRepository<Usuario, Integer>{
    
}
