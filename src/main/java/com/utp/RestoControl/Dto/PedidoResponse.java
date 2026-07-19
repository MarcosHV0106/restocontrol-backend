package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.text.Normalizer;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PedidoResponse {

    private Integer idPedido;

    private LocalDateTime fechaPedido;

    private BigDecimal total;

    private String observacion;

    private LocalDateTime fechaPago;

    private String metodoPago;

    private LocalDateTime fechaEnvioCocina;

    private LocalDateTime fechaSolicitudCuenta;

    private LocalDateTime fechaCancelacion;

    private String motivoCancelacion;

    private String clienteNombre;

    private String clienteTelefono;

    private String direccionEntrega;

    private String etapaFlujo;

    private boolean editable;

    private boolean puedeEnviarCocina;

    private boolean puedeReabrir;

    private boolean puedeSolicitarCuenta;

    private MesaResponse mesa;

    private UsuarioResponse usuario;

    private EstadoPedidoResponse estadoPedido;

    private ModalidadPedidoResponse modalidadPedido;

    private List<DetallePedidoResponse> detalles;

    public static PedidoResponse from(
            Pedido pedido) {

        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getFechaPedido(),
                pedido.getTotal(),
                pedido.getObservacion(),
                pedido.getFechaPago(),
                pedido.getMetodoPago(),
                pedido.getFechaEnvioCocina(),
                pedido.getFechaSolicitudCuenta(),
                pedido.getFechaCancelacion(),
                pedido.getMotivoCancelacion(),
                pedido.getClienteNombre(),
                pedido.getClienteTelefono(),
                pedido.getDireccionEntrega(),
                resolverEtapa(pedido),
                esEditable(pedido),
                esEditable(pedido),
                puedeReabrir(pedido),
                puedeSolicitarCuenta(pedido),
                pedido.getIdMesa() == null ? null : MesaResponse.from(pedido.getIdMesa()),
                UsuarioResponse.from(
                        pedido.getUsuario()
                ),
                EstadoPedidoResponse.from(
                        pedido.getEstadoPedido()
                ),
                ModalidadPedidoResponse.from(
                        pedido.getModalidadPedido()
                ),
                (pedido.getDetalles() == null
                        ? Collections.<com.utp.RestoControl.Entity.DetallePedido>emptyList()
                        : pedido.getDetalles())
                        .stream()
                        .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                        .map(
                                DetallePedidoResponse::from
                        )
                        .toList()
        );

    }

    private static boolean esEditable(Pedido pedido) {
        return pedido.getFechaEnvioCocina() == null && !estaCerrado(pedido);
    }

    private static boolean puedeReabrir(Pedido pedido) {
        return pedido.getFechaEnvioCocina() != null
                && pedido.getFechaConsumoInventario() == null
                && pedido.getFechaSolicitudCuenta() == null
                && "PENDIENTE".equals(normalizarEstado(pedido));
    }

    private static boolean puedeSolicitarCuenta(Pedido pedido) {
        return pedido.getFechaSolicitudCuenta() == null
                && !estaCerrado(pedido)
                && "ENTREGADO".equals(normalizarEstado(pedido));
    }

    private static boolean estaCerrado(Pedido pedido) {
        String estado = normalizarEstado(pedido);
        return "PAGADO".equals(estado) || "COBRADO".equals(estado) || "CANCELADO".equals(estado);
    }

    private static String resolverEtapa(Pedido pedido) {
        String estado = normalizarEstado(pedido);
        if ("PAGADO".equals(estado) || "COBRADO".equals(estado)) {
            return "PAGADO";
        }
        if ("CANCELADO".equals(estado)) {
            return "CANCELADO";
        }
        if (pedido.getFechaSolicitudCuenta() != null) {
            return "CUENTA_SOLICITADA";
        }
        if (pedido.getFechaEnvioCocina() == null) {
            return "BORRADOR";
        }
        return switch (estado) {
            case "EN_PREPARACION", "PREPARANDO" -> "EN_PREPARACION";
            case "LISTO" -> "LISTO";
            case "ENTREGADO" -> "ENTREGADO";
            default -> "RECIBIDO";
        };
    }

    private static String normalizarEstado(Pedido pedido) {
        String valor = pedido.getEstadoPedido() == null
                ? ""
                : pedido.getEstadoPedido().getNombreEstado();
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

}
