/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Rol;
import com.utp.RestoControl.Repository.RolRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolService {
    
    private final RolRepository repository;
    
    public List<Rol> listar() {
        return repository.findAll();
    }

    public Rol guardar(Rol rol) {
        return repository.save(rol);
    }
}
