
package com.utp.RestoControl.Controller;

import com.utp.RestoControl.Dto.UsuarioRequest;
import com.utp.RestoControl.Dto.UsuarioResponse;
import com.utp.RestoControl.Service.UsuarioService;
import java.util.List;
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
@RequestMapping("api/usuarios")

@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;
    
    @GetMapping
    public List<UsuarioResponse> listar(){
        return service.listar()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @GetMapping("{id}")
    public UsuarioResponse buscarPorId(@PathVariable Integer id) {
        return UsuarioResponse.from(service.buscarPorId(id));
    }
    
    @PostMapping
    public ResponseEntity<UsuarioResponse> guardar(@RequestBody UsuarioRequest usuarioPedido){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UsuarioResponse.from(service.guardar(usuarioPedido)));
    }

    @PutMapping("{id}")
    public UsuarioResponse actualizar(
            @PathVariable Integer id,
            @RequestBody UsuarioRequest usuarioPedido) {
        return UsuarioResponse.from(service.actualizar(id, usuarioPedido));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
