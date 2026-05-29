
package com.utp.RestoControl.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EstadoPedidoVistaController {

    @GetMapping("/estadopedido")
    public String formulario() {
        return "estadopedido";
    }
}
