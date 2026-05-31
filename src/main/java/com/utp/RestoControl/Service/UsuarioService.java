
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UsuarioService {
    
    private final UsuarioRepository repository;
    
    
    public List<Usuario> listar(){
        return repository.findAll();
    }
    public Usuario guardar(Usuario usuario){
        return repository.save(usuario);
    }
}
