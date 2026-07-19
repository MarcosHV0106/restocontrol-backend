package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ActualizarLoteRequest;
import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Dto.LoteInsumoResponse;
import com.utp.RestoControl.Dto.RetirarLoteRequest;
import com.utp.RestoControl.Service.AlertaInventarioService;
import com.utp.RestoControl.Service.LoteInsumoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoteInsumoController {

    private final LoteInsumoService loteService;
    private final AlertaInventarioService alertaService;

    @GetMapping("/insumos/{idInsumo}/lotes")
    public ResponseEntity<List<LoteInsumoResponse>> listar(@PathVariable Integer idInsumo) {
        return ResponseEntity.ok(loteService.listarPorInsumo(idInsumo).stream()
                .map(LoteInsumoResponse::from).toList());
    }

    @PostMapping("/insumos/{idInsumo}/lotes")
    public ResponseEntity<LoteInsumoResponse> crear(
            @PathVariable Integer idInsumo,
            @RequestBody LoteInsumoRequest request) {
        LoteInsumoResponse response = LoteInsumoResponse.from(loteService.crear(idInsumo, request));
        alertaService.sincronizar();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/lotes/{idLote}")
    public ResponseEntity<LoteInsumoResponse> actualizar(
            @PathVariable Integer idLote,
            @RequestBody ActualizarLoteRequest request) {
        LoteInsumoResponse response = LoteInsumoResponse.from(loteService.actualizar(idLote, request));
        alertaService.sincronizar();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/lotes/{idLote}/retirar")
    public ResponseEntity<LoteInsumoResponse> retirar(
            @PathVariable Integer idLote,
            @RequestBody(required = false) RetirarLoteRequest request) {
        LoteInsumoResponse response = LoteInsumoResponse.from(loteService.retirar(idLote, request));
        alertaService.sincronizar();
        return ResponseEntity.ok(response);
    }
}
