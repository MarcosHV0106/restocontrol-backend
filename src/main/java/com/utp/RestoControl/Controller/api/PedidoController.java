package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Dto.PedidoResponse;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Service.PedidoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/pedidos")

@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @PostMapping
    public ResponseEntity<PedidoResponse> guardar(
            @RequestBody PedidoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        PedidoResponse.from(
                                service.guardar(request)
                        )
                );

    }

    @GetMapping("/mesa/{idMesa}")
    public ResponseEntity<PedidoResponse> buscarPorMesa(
            @PathVariable Integer idMesa) {

        Pedido pedido
                = service.buscarUltimoPorMesa(idMesa);

        if (pedido == null) {

            return ResponseEntity.ok(null);

        }

        return ResponseEntity.ok(
                PedidoResponse.from(pedido)
        );
    }

}
