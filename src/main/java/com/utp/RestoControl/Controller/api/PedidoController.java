package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Dto.PedidoResponse;
import com.utp.RestoControl.Dto.AnularPedidoRequest;
import com.utp.RestoControl.Dto.CambiarMesaPedidoRequest;
import com.utp.RestoControl.Dto.ObservacionPedidoRequest;
import com.utp.RestoControl.Dto.ResponsablePedidoResponse;
import com.utp.RestoControl.Dto.TransferirPedidoRequest;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Service.PedidoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/pedidos")

@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar() {
        return ResponseEntity.ok(
                service.listarPedidosSegunRol()
                        .stream()
                        .map(PedidoResponse::from)
                        .toList()
        );
    }

    @PostMapping("crear")
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

    @GetMapping("/{idPedido}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Integer idPedido) {

        return ResponseEntity.ok(
                PedidoResponse.from(
                        service.buscarPorIdSegunRol(idPedido)
                )
        );
    }

    @PutMapping("/{idPedido}")
    public ResponseEntity<PedidoResponse> actualizar(
            @PathVariable Integer idPedido,
            @RequestBody PedidoRequest request) {

        return ResponseEntity.ok(
                PedidoResponse.from(
                        service.actualizar(idPedido, request)
                )
        );
    }

    @PostMapping("/{idPedido}/enviar-cocina")
    public ResponseEntity<PedidoResponse> enviarACocina(@PathVariable Integer idPedido) {
        return ResponseEntity.ok(PedidoResponse.from(service.enviarACocina(idPedido)));
    }

    @PostMapping("/{idPedido}/reabrir")
    public ResponseEntity<PedidoResponse> reabrir(@PathVariable Integer idPedido) {
        return ResponseEntity.ok(PedidoResponse.from(service.reabrir(idPedido)));
    }

    @PutMapping("/{idPedido}/observacion")
    public ResponseEntity<PedidoResponse> actualizarObservacion(
            @PathVariable Integer idPedido,
            @RequestBody ObservacionPedidoRequest request) {
        return ResponseEntity.ok(PedidoResponse.from(
                service.actualizarObservacion(idPedido, request == null ? null : request.getObservacion())
        ));
    }

    @PostMapping("/{idPedido}/solicitar-cuenta")
    public ResponseEntity<PedidoResponse> solicitarCuenta(@PathVariable Integer idPedido) {
        return ResponseEntity.ok(PedidoResponse.from(service.solicitarCuenta(idPedido)));
    }

    @PostMapping("/{idPedido}/anular")
    public ResponseEntity<PedidoResponse> anular(
            @PathVariable Integer idPedido,
            @RequestBody AnularPedidoRequest request) {
        return ResponseEntity.ok(PedidoResponse.from(
                service.anular(idPedido, request == null ? null : request.getMotivo())
        ));
    }

    @PutMapping("/{idPedido}/mesa")
    public ResponseEntity<PedidoResponse> cambiarMesa(
            @PathVariable Integer idPedido,
            @RequestBody CambiarMesaPedidoRequest request) {
        return ResponseEntity.ok(PedidoResponse.from(
                service.cambiarMesa(idPedido, request == null ? null : request.getIdMesaDestino())
        ));
    }

    @PutMapping("/{idPedido}/responsable")
    public ResponseEntity<PedidoResponse> transferirResponsable(
            @PathVariable Integer idPedido,
            @RequestBody TransferirPedidoRequest request) {
        return ResponseEntity.ok(PedidoResponse.from(
                service.transferirResponsable(
                        idPedido,
                        request == null ? null : request.getIdUsuarioDestino()
                )
        ));
    }

    @GetMapping("/responsables")
    public ResponseEntity<List<ResponsablePedidoResponse>> listarResponsables() {
        return ResponseEntity.ok(
                service.listarResponsables().stream()
                        .map(ResponsablePedidoResponse::from)
                        .toList()
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
