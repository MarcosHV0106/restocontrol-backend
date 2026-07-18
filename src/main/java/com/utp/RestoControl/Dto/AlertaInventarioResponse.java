package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.AlertaInventario;
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
    private Integer idLote;
    private String codigoLote;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaRevision;
    private LocalDateTime fechaAtencion;
    private String accion;
    private String observacion;
    private String usuarioAtencion;

    public static AlertaInventarioResponse from(AlertaInventario alerta) {
        String usuario = alerta.getUsuarioAtencion() == null ? null
                : (alerta.getUsuarioAtencion().getNombre() + " " + alerta.getUsuarioAtencion().getApellido()).trim();
        return new AlertaInventarioResponse(
                alerta.getIdAlerta(), alerta.getTipo(), alerta.getEstado(), alerta.getDetalle(),
                alerta.getInsumo().getIdInsumo(), alerta.getInsumo().getNombreInsumo(),
                alerta.getLote() == null ? null : alerta.getLote().getIdLote(),
                alerta.getLote() == null ? null : alerta.getLote().getCodigo(),
                alerta.getFechaGeneracion(), alerta.getFechaRevision(), alerta.getFechaAtencion(),
                alerta.getAccion(), alerta.getObservacion(), usuario
        );
    }
}
