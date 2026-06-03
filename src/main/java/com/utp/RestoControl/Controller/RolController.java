/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Service.RolService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    
    @PostMapping
    public Rol guardar(@RequestBody Rol rol){
        return service.guardar(rol);
    } 
    
}
