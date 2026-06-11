
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.EstadoPedidoRequest;
import com.utp.RestoControl.Dto.EstadoPedidoResponse;
import com.utp.RestoControl.Service.EstadoPedidoService;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/estadospedidos")

@RequiredArgsConstructor
public class EstadoPedidoController {

    private final EstadoPedidoService service;

    @GetMapping
    public List<EstadoPedidoResponse> listar() {

        return service.listar()
                .stream()
                .map(EstadoPedidoResponse::from)
                .toList();

    }

    @GetMapping("{id}")
    public EstadoPedidoResponse buscarPorId(
            @PathVariable Integer id) {

        return EstadoPedidoResponse.from(
                service.buscarPorId(id)
        );

    }

    @PostMapping
    public ResponseEntity<EstadoPedidoResponse> guardar(
            @RequestBody EstadoPedidoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        EstadoPedidoResponse.from(
                                service.guardar(request)
                        )
                );

    }

    @PutMapping("{id}")
    public EstadoPedidoResponse actualizar(
            @PathVariable Integer id,
            @RequestBody EstadoPedidoRequest request) {

        return EstadoPedidoResponse.from(
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