package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Service.ModalidadPedidoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/modalidadespedidos")

@RequiredArgsConstructor
public class ModalidadPedidoController {
    private final ModalidadPedidoService service;
    
    @GetMapping
    public List<ModalidadPedido> listar(){
        return service.listar();
    }
    
    @PostMapping
    public ModalidadPedido guardar(@RequestBody ModalidadPedido modalidadPedido){
        return service.guardar(modalidadPedido);
    }
}
