package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.CobroRequest;
import com.utp.RestoControl.Dto.CobroResponse;
import com.utp.RestoControl.Dto.PedidoResponse;
import com.utp.RestoControl.Service.CobroService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final CobroService cobroService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoResponse>> listarPendientes() {
        return ResponseEntity.ok(
                cobroService.listarPendientesSegunRol()
                        .stream()
                        .map(PedidoResponse::from)
                        .toList()
        );
    }

    @GetMapping("/pedidos/{idPedido}")
    public ResponseEntity<PedidoResponse> buscarPedido(
            @PathVariable Integer idPedido
    ) {
        return ResponseEntity.ok(
                PedidoResponse.from(cobroService.buscarPedidoParaCobro(idPedido))
        );
    }

    @PostMapping("/pedidos/{idPedido}")
    public ResponseEntity<CobroResponse> procesar(
            @PathVariable Integer idPedido,
            @RequestBody CobroRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CobroResponse.from(cobroService.procesarCobro(idPedido, request)));
    }

    @GetMapping("/{idCobro}")
    public ResponseEntity<CobroResponse> buscarCobro(
            @PathVariable Integer idCobro
    ) {
        return ResponseEntity.ok(CobroResponse.from(cobroService.buscarPorId(idCobro)));
    }
}
