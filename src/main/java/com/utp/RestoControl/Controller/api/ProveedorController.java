package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ProveedorRequest;
import com.utp.RestoControl.Dto.ProveedorResponse;
import com.utp.RestoControl.Service.ProveedorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService service;

    @GetMapping
    public List<ProveedorResponse> listar() {
        return service.listar().stream().map(ProveedorResponse::from).toList();
    }

    @GetMapping("/{idProveedor}")
    public ProveedorResponse buscar(@PathVariable Integer idProveedor) {
        return ProveedorResponse.from(service.buscarPorId(idProveedor));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@RequestBody ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponse.from(service.crear(request)));
    }

    @PutMapping("/{idProveedor}")
    public ProveedorResponse actualizar(
            @PathVariable Integer idProveedor,
            @RequestBody ProveedorRequest request) {
        return ProveedorResponse.from(service.actualizar(idProveedor, request));
    }

    @DeleteMapping("/{idProveedor}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer idProveedor) {
        service.eliminar(idProveedor);
        return ResponseEntity.noContent().build();
    }
}
