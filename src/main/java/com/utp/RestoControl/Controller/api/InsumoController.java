
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Service.InsumoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/insumos")

@RequiredArgsConstructor
public class InsumoController {
    private final InsumoService service;
    
    @GetMapping
    public List<Insumo> listar(){
        return service.listar();
    }
    
    @PostMapping
    public Insumo guardar(@RequestBody Insumo insumo){
        return service.guardar(insumo);
    }
}
