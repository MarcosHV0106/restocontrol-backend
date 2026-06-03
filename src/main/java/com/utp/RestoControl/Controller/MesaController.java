
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Service.MesaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping("{id}")
    public Mesa buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }
    
    @PostMapping
    public ResponseEntity<Mesa> guardar(@RequestBody Mesa mesa){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.guardar(mesa));
    }

    @PutMapping("{id}")
    public Mesa actualizar(@PathVariable Integer id, @RequestBody Mesa mesa) {
        return service.actualizar(id, mesa);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
