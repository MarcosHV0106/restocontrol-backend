package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.EstimacionDiariaItemRequest;
import com.utp.RestoControl.Dto.EstimacionDiariaResponse;
import com.utp.RestoControl.Service.EstimacionDiariaService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/estimaciones-diarias")
public class EstimacionDiariaController {

    private final EstimacionDiariaService service;

    @GetMapping("/{fecha}")
    public EstimacionDiariaResponse consultar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return service.consultar(fecha);
    }

    @PutMapping("/{fecha}")
    public EstimacionDiariaResponse guardar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody List<EstimacionDiariaItemRequest> estimacion
    ) {
        return service.guardar(fecha, estimacion);
    }

    @PostMapping("/{fecha}/validar")
    public EstimacionDiariaResponse validar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody List<EstimacionDiariaItemRequest> estimacion
    ) {
        return service.validar(fecha, estimacion);
    }
}
