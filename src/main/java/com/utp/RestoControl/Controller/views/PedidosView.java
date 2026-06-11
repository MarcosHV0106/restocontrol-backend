/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.utp.RestoControl.Controller.views;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PedidosView {

    @GetMapping("/pedidos")
    public String pedidos(
            @RequestParam(required = false) Integer idMesa,
            @RequestParam(required = false) Integer personas,
            Model model) {

        model.addAttribute("idMesa", idMesa);
        model.addAttribute("personas", personas);

        return "pedidos";

    }

}
