/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Entity.CategoriaAlimento;
import com.utp.RestoControl.Service.CategoriaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("{id}/estado")
    public ResponseEntity<Void> cambiarEstado(
            @PathVariable Integer id) {

        service.cambiarEstado(id);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
