package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Dto.AuditoriaOpcionesResponse;
import com.utp.RestoControl.Dto.AuditoriaPaginaResponse;
import com.utp.RestoControl.Service.AuditoriaOperacionService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaOperacionService service;

    @GetMapping
    public ResponseEntity<AuditoriaPaginaResponse> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer idUsuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano
    ) {
        return ResponseEntity.ok(service.consultar(
                desde, hasta, idUsuario, modulo, accion, resultado, texto, pagina, tamano));
    }

    @GetMapping("opciones")
    public ResponseEntity<AuditoriaOpcionesResponse> opciones() {
        return ResponseEntity.ok(service.obtenerOpciones());
    }
}
