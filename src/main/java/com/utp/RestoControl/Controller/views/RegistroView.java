package com.utp.RestoControl.Controller.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RegistroView {

    @GetMapping
    public String registrar() {
        return "registro";
    }
}
