package com.utp.RestoControl.Controller.api;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Service.AlimentoService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/alimentos")

@RequiredArgsConstructor
public class AlimentoController {

    private final AlimentoService service;

    @GetMapping
    public List<Alimento> listar() {
        return service.listar();
    }

    @PostMapping
    public Alimento guardar(
            @RequestBody Alimento alimento) {

        return service.guardar(alimento);
    }
}