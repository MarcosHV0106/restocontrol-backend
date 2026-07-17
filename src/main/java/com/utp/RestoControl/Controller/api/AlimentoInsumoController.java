package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.AlimentoInsumoRequest;
import com.utp.RestoControl.Dto.AlimentoInsumoResponse;
import com.utp.RestoControl.Entity.AlimentoInsumo;
import com.utp.RestoControl.Service.AlimentoInsumoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alimento-insumo")
@RequiredArgsConstructor
public class AlimentoInsumoController {

    private final AlimentoInsumoService service;

    @GetMapping
    public ResponseEntity<List<AlimentoInsumoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlimentoInsumoResponse> buscar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<AlimentoInsumoResponse> guardar(
            @RequestBody AlimentoInsumoRequest request) {

        return ResponseEntity.ok(service.guardar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alimento/{id}")
    public ResponseEntity<List<AlimentoInsumoResponse>>
            listarPorAlimento(@PathVariable Integer id) {

        return ResponseEntity.ok(
                service.listarPorAlimento(id));

    }

    @GetMapping("/insumo/{id}")
    public ResponseEntity<List<AlimentoInsumoResponse>>
            listarPorInsumo(@PathVariable Integer id) {

        return ResponseEntity.ok(
                service.listarPorInsumo(id));

    }

}
