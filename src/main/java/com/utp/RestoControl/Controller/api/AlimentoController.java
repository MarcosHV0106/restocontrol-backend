package com.utp.RestoControl.Controller.api;
import com.utp.RestoControl.Dto.AlimentoRequest;
import com.utp.RestoControl.Dto.AlimentoResponse;
import com.utp.RestoControl.Service.AlimentoService;
import com.utp.RestoControl.Dto.RecetaAlimentoRequest;
import com.utp.RestoControl.Dto.RecetaAlimentoResponse;
import com.utp.RestoControl.Service.RecetaAlimentoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/alimentos")

@RequiredArgsConstructor
public class AlimentoController {

    private final AlimentoService service;
    private final RecetaAlimentoService recetaService;

    @GetMapping
    public List<AlimentoResponse> listar(@RequestParam(required = false) Integer idCategoria) {
        return (idCategoria == null ? service.listar() : service.listarPorCategoria(idCategoria))
                .stream()
                .map(AlimentoResponse::from)
                .toList();
    }

    @GetMapping("{id}")
    public AlimentoResponse buscarPorId(@PathVariable Integer id) {
        return AlimentoResponse.from(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<AlimentoResponse> guardar(
            @RequestBody AlimentoRequest alimento) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AlimentoResponse.from(service.guardar(alimento)));
    }

    @PutMapping("{id}")
    public AlimentoResponse actualizar(
            @PathVariable Integer id,
            @RequestBody AlimentoRequest alimento) {
        return AlimentoResponse.from(service.actualizar(id, alimento));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}/receta")
    public List<RecetaAlimentoResponse> obtenerReceta(@PathVariable Integer id) {
        return recetaService.listar(id).stream()
                .map(RecetaAlimentoResponse::from)
                .toList();
    }

    @PutMapping("{id}/receta")
    public List<RecetaAlimentoResponse> reemplazarReceta(
            @PathVariable Integer id,
            @RequestBody List<RecetaAlimentoRequest> receta) {
        return recetaService.reemplazar(id, receta).stream()
                .map(RecetaAlimentoResponse::from)
                .toList();
    }
}
