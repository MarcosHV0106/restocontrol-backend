
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Service.EstadoPedidoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/estadospedidos")

@RequiredArgsConstructor
public class EstadoPedidoController {
    private final EstadoPedidoService service;
    
    @GetMapping
    public List<EstadoPedido> listar(){
        return service.listar();
    }
    
    @PostMapping
    public EstadoPedido guardar(@RequestBody EstadoPedido estadoPedido){
        return service.guardar(estadoPedido);
    }
}
