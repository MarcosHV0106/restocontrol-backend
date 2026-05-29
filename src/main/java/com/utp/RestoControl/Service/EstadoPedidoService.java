
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EstadoPedidoService {
    
    private final EstadoPedidoRepository repository;
    
    public List<EstadoPedido> listar(){
        return repository.findAll();
    }
    public EstadoPedido guardar(EstadoPedido estadoPedido){
        return repository.save(estadoPedido);
    }
}
