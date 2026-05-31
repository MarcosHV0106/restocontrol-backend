
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Repository.InsumoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsumoService {
    
    private final InsumoRepository repository;
    
    public List<Insumo> listar(){
        return repository.findAll();
    }
    public Insumo guardar(Insumo insumo){
        return repository.save(insumo);
    }
}
