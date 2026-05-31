
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Repository.MesaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MesaService {
    
    private final MesaRepository repository;
    
    public List<Mesa> listar(){
        return repository.findAll();
    }
    public Mesa guardar(Mesa mesa){
        return repository.save(mesa);
    }
}
