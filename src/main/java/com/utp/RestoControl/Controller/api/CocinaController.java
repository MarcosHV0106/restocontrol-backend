package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ActualizarEstadoCocinaRequest;
import com.utp.RestoControl.Dto.PedidoCocinaResponse;
import com.utp.RestoControl.Service.CocinaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cocina")
@RequiredArgsConstructor
public class CocinaController {

    private final CocinaService cocinaService;

    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoCocinaResponse>> listarPedidos() {
        return ResponseEntity.ok(
                cocinaService.listarPedidos()
                        .stream()
                        .map(PedidoCocinaResponse::from)
                        .toList()
        );
    }

    @PutMapping("/pedidos/{idPedido}/estado")
    public ResponseEntity<PedidoCocinaResponse> actualizarEstado(
            @PathVariable Integer idPedido,
            @RequestBody ActualizarEstadoCocinaRequest request
    ) {
        return ResponseEntity.ok(
                PedidoCocinaResponse.from(
                        cocinaService.actualizarEstado(
                                idPedido,
                                request == null ? null : request.getEstado()
                        )
                )
        );
    }
}
