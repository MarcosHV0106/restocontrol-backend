
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Service.MesaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/mesas")

@RequiredArgsConstructor
public class MesaController {
    private final MesaService service;
    
    @GetMapping
    public List<Mesa> listar(){
        return service.listar();
    }
    
    @PostMapping
    public Mesa guardar(@RequestBody Mesa mesa){
        return service.guardar(mesa);
    }
}
