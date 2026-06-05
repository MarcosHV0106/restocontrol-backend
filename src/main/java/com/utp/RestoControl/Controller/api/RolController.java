/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Service.RolService;
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
@RequestMapping("api/roles")

@RequiredArgsConstructor
public class RolController {
    
    private final RolService service;
    
    @GetMapping
    public List<Rol> listar(){
        return service.listar();
    }

    @GetMapping("{id}")
    public Rol buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }
    
    @PostMapping
    public ResponseEntity<Rol> guardar(@RequestBody Rol rol){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.guardar(rol));
    } 

    @PutMapping("{id}")
    public Rol actualizar(@PathVariable Integer id, @RequestBody Rol rol) {
        return service.actualizar(id, rol);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
}
