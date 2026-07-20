package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.AlertaInventario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertaInventarioResponse {
    private Integer idAlerta;
    private String tipo;
    private String estado;
    private String detalle;
    private Integer idInsumo;
    private String nombreInsumo;
    private String unidadMedida;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal cantidadSugeridaReposicion;
    private Integer idLote;
    private String codigoLote;
    private BigDecimal cantidadLote;
    private LocalDate fechaVencimiento;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaRevision;
    private LocalDateTime fechaAtencion;
    private String accion;
    private String observacion;
    private String usuarioAtencion;

    public static AlertaInventarioResponse from(AlertaInventario alerta) {
        String usuario = alerta.getUsuarioAtencion() == null ? null
                : (alerta.getUsuarioAtencion().getNombre() + " " + alerta.getUsuarioAtencion().getApellido()).trim();
        BigDecimal stockActual = valorSeguro(alerta.getInsumo().getStockActual());
        BigDecimal stockMinimo = valorSeguro(alerta.getInsumo().getStockMinimo());
        BigDecimal cantidadSugerida = "STOCK_BAJO".equals(alerta.getTipo())
                ? stockMinimo.subtract(stockActual).max(BigDecimal.ZERO).add(BigDecimal.ONE)
                : null;
        return new AlertaInventarioResponse(
                alerta.getIdAlerta(), alerta.getTipo(), alerta.getEstado(), alerta.getDetalle(),
                alerta.getInsumo().getIdInsumo(), alerta.getInsumo().getNombreInsumo(),
                alerta.getInsumo().getUnidadMedida(), stockActual, stockMinimo, cantidadSugerida,
                alerta.getLote() == null ? null : alerta.getLote().getIdLote(),
                alerta.getLote() == null ? null : alerta.getLote().getCodigo(),
                alerta.getLote() == null ? null : alerta.getLote().getCantidadActual(),
                alerta.getLote() == null ? null : alerta.getLote().getFechaVencimiento(),
                alerta.getFechaGeneracion(), alerta.getFechaRevision(), alerta.getFechaAtencion(),
                alerta.getAccion(), alerta.getObservacion(), usuario
        );
    }

    private static BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
