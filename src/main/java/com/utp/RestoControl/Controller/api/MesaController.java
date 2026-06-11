package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.MesaRequest;
import com.utp.RestoControl.Dto.MesaResponse;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Service.MesaService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping("api/mesas")

@RequiredArgsConstructor
public class MesaController {

    private final MesaService service;

    @GetMapping
    public List<MesaResponse> listar() {

        return service.listar()
                .stream()
                .map(MesaResponse::from)
                .toList();

    }

    @GetMapping("{id}")
    public MesaResponse buscarPorId(
            @PathVariable Integer id) {

        return MesaResponse.from(
                service.buscarPorId(id)
        );

    }

    @PostMapping
    public ResponseEntity<MesaResponse> guardar(
            @RequestBody MesaRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        MesaResponse.from(
                                service.guardar(request)
                        )
                );

    }

    @PutMapping("{id}")
    public MesaResponse actualizar(
            @PathVariable Integer id,
            @RequestBody MesaRequest request) {

        return MesaResponse.from(
                service.actualizar(id, request)
        );

    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resumen")
    public Map<String, Integer> resumen() {

        Map<String, Integer> datos = new HashMap<>();

        datos.put("libres", service.contarMesasLibres());
        datos.put("ocupadas", service.contarMesasOcupadas());
        datos.put("reservadas", service.contarMesasReservadas());
        datos.put("cobradas", service.contarMesasPorCobrar());

        return datos;
    }
}
