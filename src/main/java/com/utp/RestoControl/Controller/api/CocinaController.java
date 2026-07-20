package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ActualizarEstadoCocinaRequest;
import com.utp.RestoControl.Dto.ActualizarDisponibilidadCocinaRequest;
import com.utp.RestoControl.Dto.AlimentoResponse;
import com.utp.RestoControl.Dto.HistorialTurnoCocinaResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cocina")
@RequiredArgsConstructor
public class CocinaController {

    private final CocinaService cocinaService;

    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoCocinaResponse>> listarPedidos() {
        return ResponseEntity.ok(cocinaService.listarPedidos());
    }

    @GetMapping("/historial")
    public ResponseEntity<HistorialTurnoCocinaResponse> obtenerHistorial(
            @RequestParam(defaultValue = "ACTUAL") String turno
    ) {
        return ResponseEntity.ok(cocinaService.obtenerHistorialTurno(turno));
    }

    @GetMapping("/productos")
    public ResponseEntity<List<AlimentoResponse>> listarProductos() {
        return ResponseEntity.ok(cocinaService.listarProductos());
    }

    @PutMapping("/productos/{idAlimento}/disponibilidad")
    public ResponseEntity<AlimentoResponse> actualizarDisponibilidadProducto(
            @PathVariable Integer idAlimento,
            @RequestBody ActualizarDisponibilidadCocinaRequest request
    ) {
        return ResponseEntity.ok(
                cocinaService.actualizarDisponibilidadProducto(idAlimento, request)
        );
    }

    @PutMapping("/pedidos/{idPedido}/estado")
    public ResponseEntity<PedidoCocinaResponse> actualizarEstado(
            @PathVariable Integer idPedido,
            @RequestBody ActualizarEstadoCocinaRequest request
    ) {
        return ResponseEntity.ok(
                cocinaService.presentar(
                        cocinaService.actualizarEstado(
                                idPedido,
                                request == null ? null : request.getEstado()
                        )
                )
        );
    }
}
