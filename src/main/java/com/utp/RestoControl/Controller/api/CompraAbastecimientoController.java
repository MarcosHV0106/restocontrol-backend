package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.CompraAbastecimientoRequest;
import com.utp.RestoControl.Dto.CompraAbastecimientoResponse;
import com.utp.RestoControl.Service.CompraAbastecimientoService;
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
@RequiredArgsConstructor
@RequestMapping("/api/compras-abastecimiento")
public class CompraAbastecimientoController {

    private final CompraAbastecimientoService service;

    @GetMapping
    public List<CompraAbastecimientoResponse> listar() {
        return service.listar().stream().map(CompraAbastecimientoResponse::from).toList();
    }

    @GetMapping("/{idCompra}")
    public CompraAbastecimientoResponse buscar(@PathVariable Integer idCompra) {
        return CompraAbastecimientoResponse.from(service.buscarPorId(idCompra));
    }

    @PostMapping
    public ResponseEntity<CompraAbastecimientoResponse> registrar(
            @RequestBody CompraAbastecimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CompraAbastecimientoResponse.from(service.registrar(request)));
    }
}
