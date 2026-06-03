/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Service.CategoriaService;
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
@RequestMapping("api/categorias")

@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public List<CategoriaAlimento> listar() {
        return service.listar();
    }

    @GetMapping("{id}")
    public CategoriaAlimento buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaAlimento> guardar(
            @RequestBody CategoriaAlimento categoria) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.guardar(categoria));
    }

    @PutMapping("{id}")
    public CategoriaAlimento actualizar(
            @PathVariable Integer id,
            @RequestBody CategoriaAlimento categoria) {
        return service.actualizar(id, categoria);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
