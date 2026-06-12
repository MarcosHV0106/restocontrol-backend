package com.utp.RestoControl.Controller.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConfiguracionView {

    @GetMapping("/configuracion")
    public String configuracion() {
        return "disponible";
    }

}
