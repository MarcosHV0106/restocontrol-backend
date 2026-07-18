package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.LoteInsumo;
import java.time.LocalDate;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoteInsumoResponse {
    private Integer idLote;
    private Integer idInsumo;
    private String nombreInsumo;
    private String codigo;
    private BigDecimal cantidadInicial;
    private BigDecimal cantidadActual;
    private LocalDate fechaIngreso;
    private LocalDate fechaVencimiento;
    private String estado;

    public static LoteInsumoResponse from(LoteInsumo lote) {
        return new LoteInsumoResponse(
                lote.getIdLote(), lote.getInsumo().getIdInsumo(),
                lote.getInsumo().getNombreInsumo(), lote.getCodigo(),
                lote.getCantidadInicial(), lote.getCantidadActual(),
                lote.getFechaIngreso(), lote.getFechaVencimiento(), lote.getEstado()
        );
    }
}
