
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {
    
    private final MovimientoInventarioRepository repository;
    
    public List<MovimientoInventario> listar(){
        return repository.findAll();
    }
    public MovimientoInventario guardar(MovimientoInventario movimientoInventario){
        return repository.save(movimientoInventario);
    }
}
