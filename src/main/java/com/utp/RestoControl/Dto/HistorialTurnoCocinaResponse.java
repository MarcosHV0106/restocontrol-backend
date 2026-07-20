package com.utp.RestoControl.Dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HistorialTurnoCocinaResponse {

    private String turno;
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private int totalPedidos;
    private int totalPlatos;
    private int promedioPreparacionMinutos;
    private List<PedidoCocinaResponse> pedidos;
}
