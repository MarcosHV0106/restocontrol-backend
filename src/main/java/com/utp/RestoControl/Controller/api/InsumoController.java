package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Service.InsumoService;
import com.utp.RestoControl.Service.AlertaInventarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("api/insumos")

@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService service;
    private final AlertaInventarioService alertaService;

    @GetMapping
    public ResponseEntity<List<Insumo>> listar() {

        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<Insumo> guardar(@RequestBody Insumo insumo) {
        Insumo guardado = service.guardar(insumo);
        alertaService.sincronizar();
        return ResponseEntity.ok(guardado);
    }

    @GetMapping("/{id}")
    public Insumo buscar(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public Insumo actualizar(@PathVariable Integer id,
            @RequestBody Insumo insumo) {
        Insumo actualizado = service.actualizar(id, insumo);
        alertaService.sincronizar();
        return actualizado;
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        alertaService.sincronizar();
    }
}
