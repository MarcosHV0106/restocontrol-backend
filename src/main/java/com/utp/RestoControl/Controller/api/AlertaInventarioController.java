package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.AlertaInventarioResponse;
import com.utp.RestoControl.Dto.AtencionAlertaRequest;
import com.utp.RestoControl.Dto.ResumenAlertasResponse;
import com.utp.RestoControl.Service.AlertaInventarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaInventarioController {

    private final AlertaInventarioService service;

    @GetMapping
    public ResponseEntity<List<AlertaInventarioResponse>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listar(texto, tipo, estado).stream()
                .map(AlertaInventarioResponse::from).toList());
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenAlertasResponse> resumen() {
        return ResponseEntity.ok(service.resumen());
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<Void> sincronizar() {
        service.sincronizar();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idAlerta}/atenciones")
    public ResponseEntity<AlertaInventarioResponse> atender(
            @PathVariable Integer idAlerta,
            @RequestBody AtencionAlertaRequest request) {
        return ResponseEntity.ok(AlertaInventarioResponse.from(service.atender(idAlerta, request)));
    }
}
