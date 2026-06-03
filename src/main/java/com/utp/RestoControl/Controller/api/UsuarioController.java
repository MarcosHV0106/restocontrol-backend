
package com.utp.RestoControl.Controller.api;

import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Service.UsuarioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/usuarios")

@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;
    
    @GetMapping
    public List<Usuario> listar(){
        return service.listar();
    }
    
    @PostMapping
    public Usuario guardar(@RequestBody Usuario usuarioPedido){
        return service.guardar(usuarioPedido);
    }
}
