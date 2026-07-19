package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CocinaService {

    private static final int HORAS_HISTORIAL_ENTREGADOS = 12;

    private final PedidoRepository pedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ConsumoInventarioService consumoInventarioService;

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findParaCocina(
                LocalDateTime.now().minusHours(HORAS_HISTORIAL_ENTREGADOS)
        );
    }

    @Transactional
    public Pedido actualizarEstado(Integer idPedido, String estadoSolicitado) {
        Preconditions.checkArgument(idPedido != null, "El pedido es obligatorio.");
        EstadoCocina destino = EstadoCocina.desdeSolicitud(estadoSolicitado);

        Pedido pedido = pedidoRepository.findActivoParaCocina(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));

        if (pedido.getFechaEnvioCocina() == null) {
            throw new ConflictException("El pedido aun no fue enviado a Cocina.");
        }

        EstadoCocina actual = EstadoCocina.desdePersistencia(
                pedido.getEstadoPedido() == null ? null : pedido.getEstadoPedido().getNombreEstado()
        );
        EstadoCocina esperado = actual.siguiente();
        if (esperado == null || esperado != destino) {
            throw new ConflictException(
                    "El pedido cambio de estado. Actualiza el tablero antes de continuar."
            );
        }

        if (pedido.getFechaConsumoInventario() == null) {
            consumoInventarioService.consumirParaPedido(pedido);
        }

        LocalDateTime ahora = LocalDateTime.now();
        switch (destino) {
            case EN_PREPARACION -> pedido.setFechaInicioPreparacion(ahora);
            case LISTO -> {
                if (pedido.getFechaInicioPreparacion() == null) {
                    pedido.setFechaInicioPreparacion(ahora);
                }
                pedido.setFechaListo(ahora);
            }
            case ENTREGADO -> {
                if (pedido.getFechaListo() == null) {
                    pedido.setFechaListo(ahora);
                }
                pedido.setFechaEntregado(ahora);
            }
            default -> throw new ConflictException("La transición solicitada no está permitida.");
        }

        pedido.setEstadoPedido(buscarEstadoPersistente(destino));
        return pedidoRepository.save(pedido);
    }

    private EstadoPedido buscarEstadoPersistente(EstadoCocina estadoCocina) {
        return estadoPedidoRepository.findByEliminadoFalse()
                .stream()
                .filter(estado -> estadoCocina.coincide(estado.getNombreEstado()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el estado requerido para Cocina: " + estadoCocina.etiqueta() + "."
                ));
    }

    private enum EstadoCocina {
        RECIBIDO("RECIBIDO"),
        EN_PREPARACION("EN PREPARACION"),
        LISTO("LISTO"),
        ENTREGADO("ENTREGADO");

        private final String etiqueta;

        EstadoCocina(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        static EstadoCocina desdeSolicitud(String valor) {
            Preconditions.checkArgument(
                    valor != null && !valor.isBlank(),
                    "El nuevo estado es obligatorio."
            );
            EstadoCocina estado = resolver(valor);
            Preconditions.checkArgument(
                    estado != null && estado != RECIBIDO,
                    "El nuevo estado de Cocina no es válido."
            );
            return estado;
        }

        static EstadoCocina desdePersistencia(String valor) {
            EstadoCocina estado = resolver(valor);
            if (estado == null) {
                throw new ConflictException("El pedido no tiene un estado administrable desde Cocina.");
            }
            return estado;
        }

        private static EstadoCocina resolver(String valor) {
            String normalizado = normalizar(valor);
            return switch (normalizado) {
                case "PENDIENTE", "RECIBIDO" -> RECIBIDO;
                case "EN_PREPARACION", "PREPARANDO" -> EN_PREPARACION;
                case "LISTO" -> LISTO;
                case "ENTREGADO" -> ENTREGADO;
                default -> null;
            };
        }

        boolean coincide(String valor) {
            EstadoCocina estado = resolver(valor);
            return estado == this;
        }

        EstadoCocina siguiente() {
            return switch (this) {
                case RECIBIDO -> EN_PREPARACION;
                case EN_PREPARACION -> LISTO;
                case LISTO -> ENTREGADO;
                case ENTREGADO -> null;
            };
        }

        String etiqueta() {
            return etiqueta;
        }

        private static String normalizar(String valor) {
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
}
