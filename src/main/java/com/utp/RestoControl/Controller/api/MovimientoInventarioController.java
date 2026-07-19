
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.MovimientoInventarioResponse;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Service.MovimientoInventarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/movimientosinventarios")

@RequiredArgsConstructor
public class MovimientoInventarioController {
    private final MovimientoInventarioService service;
    
    @GetMapping
    public List<MovimientoInventarioResponse> listar(){
        return service.listar().stream()
                .map(MovimientoInventarioResponse::from)
                .toList();
    }
    
    @PostMapping
    public MovimientoInventario guardar(@RequestBody MovimientoInventario movimientoInventario){
        return service.guardar(movimientoInventario);
    }
}
