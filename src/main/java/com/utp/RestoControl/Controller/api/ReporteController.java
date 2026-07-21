package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.ReporteGerencialResponse;
import com.utp.RestoControl.Dto.ReporteVentasResponse;
import com.utp.RestoControl.Service.ReporteGerencialService;
import com.utp.RestoControl.Service.ReporteService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;
    private final ReporteGerencialService gerencialService;

    @GetMapping("ventas")
    public ResponseEntity<ReporteVentasResponse> ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.obtenerVentas(desde, hasta));
    }

    @GetMapping("gerencial")
    public ResponseEntity<ReporteGerencialResponse> gerencial(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(gerencialService.obtener(desde, hasta));
    }
}
