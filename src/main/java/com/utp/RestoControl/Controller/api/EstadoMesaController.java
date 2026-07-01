
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.EstadoMesaRequest;
import com.utp.RestoControl.Dto.EstadoMesaResponse;
import com.utp.RestoControl.Service.EstadoMesaService;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/estadosmesas")

@RequiredArgsConstructor
public class EstadoMesaController {

    private final EstadoMesaService service;

    @GetMapping
    public List<EstadoMesaResponse> listar() {

        return service.listar()
                .stream()
                .map(EstadoMesaResponse::from)
                .toList();

    }

    @GetMapping("{id}")
    public EstadoMesaResponse buscarPorId(
            @PathVariable Integer id) {

        return EstadoMesaResponse.from(
                service.buscarPorId(id)
        );

    }

    @PostMapping
    public ResponseEntity<EstadoMesaResponse> guardar(
            @RequestBody EstadoMesaRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        EstadoMesaResponse.from(
                                service.guardar(request)
                        )
                );

    }

    @PutMapping("{id}")
    public EstadoMesaResponse actualizar(
            @PathVariable Integer id,
            @RequestBody EstadoMesaRequest request) {

        return EstadoMesaResponse.from(
                service.actualizar(id, request)
        );

    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id) {

        service.eliminar(id);

        return ResponseEntity.noContent().build();

    }

}