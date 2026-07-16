package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PedidoCocinaResponse {

    private Integer idPedido;
    private Integer numeroMesa;
    private String responsable;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaInicioPreparacion;
    private LocalDateTime fechaListo;
    private LocalDateTime fechaEntregado;
    private String estado;
    private String modalidad;
    private String observacion;
    private List<DetallePedidoCocinaResponse> detalles;

    public static PedidoCocinaResponse from(Pedido pedido) {
        List<DetallePedidoCocinaResponse> detalles = (pedido.getDetalles() == null
                ? Collections.<DetallePedido>emptyList()
                : pedido.getDetalles())
                .stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .map(DetallePedidoCocinaResponse::from)
                .toList();

        return new PedidoCocinaResponse(
                pedido.getIdPedido(),
                pedido.getIdMesa().getNumeroMesa(),
                nombreCompleto(pedido.getUsuario()),
                pedido.getFechaPedido(),
                pedido.getFechaInicioPreparacion(),
                pedido.getFechaListo(),
                pedido.getFechaEntregado(),
                estadoCanonico(pedido),
                pedido.getModalidadPedido().getNombreModalidad(),
                pedido.getObservacion(),
                detalles
        );
    }

    private static String nombreCompleto(Usuario usuario) {
        String nombre = (usuario.getNombre() + " " + usuario.getApellido()).trim();
        return nombre.isBlank() ? usuario.getCorreo() : nombre;
    }

    private static String estadoCanonico(Pedido pedido) {
        String estado = normalizar(pedido.getEstadoPedido().getNombreEstado());
        return switch (estado) {
            case "PENDIENTE", "RECIBIDO" -> "RECIBIDO";
            case "EN_PREPARACION", "PREPARANDO" -> "EN_PREPARACION";
            case "LISTO" -> "LISTO";
            case "ENTREGADO" -> "ENTREGADO";
            default -> estado;
        };
    }

    private static String normalizar(String valor) {
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
