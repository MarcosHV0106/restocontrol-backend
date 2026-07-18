
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {
    
    private final MovimientoInventarioRepository repository;
    
    public List<MovimientoInventario> listar(){
        return repository.findByEliminadoFalseOrderByFechaMovimientoDesc();
    }
    public MovimientoInventario guardar(MovimientoInventario movimientoInventario){
        throw new ConflictException(
                "Los movimientos se registran desde los lotes para mantener el stock sincronizado"
        );
    }
}
