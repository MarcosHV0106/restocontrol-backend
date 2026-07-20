package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ActualizarDisponibilidadCocinaRequest;
import com.utp.RestoControl.Dto.AlimentoResponse;
import com.utp.RestoControl.Dto.HistorialTurnoCocinaResponse;
import com.utp.RestoControl.Dto.PedidoCocinaResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CocinaService {

    private static final int DIAS_HISTORICO_ESTIMACION = 30;
    private static final int MINUTOS_ESTIMADOS_POR_DEFECTO = 15;
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final PedidoRepository pedidoRepository;
    private final EstadoPedidoRepository estadoPedidoRepository;
    private final ConsumoInventarioService consumoInventarioService;
    private final AlimentoRepository alimentoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<PedidoCocinaResponse> listarPedidos() {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        TurnoCocina turnoActual = TurnoCocina.actual(ahora);
        EstimadorPreparacion estimador = construirEstimador(ahora);
        return pedidoRepository.findParaCocina(turnoActual.desde())
                .stream()
                .map(pedido -> presentar(pedido, estimador))
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoCocinaResponse presentar(Pedido pedido) {
        return presentar(pedido, construirEstimador(LocalDateTime.now(ZONA_LIMA)));
    }

    @Transactional(readOnly = true)
    public HistorialTurnoCocinaResponse obtenerHistorialTurno(String referencia) {
        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
        TurnoCocina turno = TurnoCocina.desdeReferencia(referencia, ahora);
        List<Pedido> pedidos = pedidoRepository.findHistorialCocina(turno.desde(), turno.hasta());
        EstimadorPreparacion estimador = construirEstimador(ahora);
        List<PedidoCocinaResponse> respuestas = pedidos.stream()
                .map(pedido -> presentar(pedido, estimador))
                .toList();
        List<Long> duraciones = pedidos.stream()
                .map(CocinaService::duracionPreparacion)
                .filter(minutos -> minutos != null && minutos >= 0)
                .toList();
        int promedio = duraciones.isEmpty()
                ? 0
                : (int) Math.round(duraciones.stream().mapToLong(Long::longValue).average().orElse(0));
        int totalPlatos = pedidos.stream().mapToInt(CocinaService::cantidadPlatos).sum();

        return new HistorialTurnoCocinaResponse(
                turno.nombre(),
                turno.desde(),
                turno.hasta(),
                pedidos.size(),
                totalPlatos,
                promedio,
                respuestas
        );
    }

    @Transactional(readOnly = true)
    public List<AlimentoResponse> listarProductos() {
        return alimentoRepository.findByEliminadoFalse()
                .stream()
                .map(AlimentoResponse::from)
                .toList();
    }

    @Transactional
    public AlimentoResponse actualizarDisponibilidadProducto(
            Integer idAlimento,
            ActualizarDisponibilidadCocinaRequest request
    ) {
        Preconditions.checkArgument(idAlimento != null, "El producto es obligatorio.");
        Preconditions.checkArgument(
                request != null && request.getDisponible() != null,
                "Indica la disponibilidad del producto."
        );
        boolean disponible = Boolean.TRUE.equals(request.getDisponible());
        String motivo = request.getMotivo() == null ? "" : request.getMotivo().trim();
        if (!disponible) {
            Preconditions.checkArgument(!motivo.isBlank(), "Indica el motivo del agotado temporal.");
            Preconditions.checkArgument(motivo.length() <= 200, "El motivo no puede superar 200 caracteres.");
        }

        Alimento alimento = alimentoRepository.findActivoParaCocina(idAlimento)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
        if (disponible) {
            alimento.setBloqueadoCocina(false);
            alimento.setMotivoBloqueoCocina(null);
            alimento.setFechaBloqueoCocina(null);
            alimento.setUsuarioBloqueoCocina(null);
        } else {
            alimento.setBloqueadoCocina(true);
            alimento.setMotivoBloqueoCocina(motivo);
            alimento.setFechaBloqueoCocina(LocalDateTime.now(ZONA_LIMA));
            alimento.setUsuarioBloqueoCocina(obtenerUsuarioActual());
        }
        return AlimentoResponse.from(alimentoRepository.save(alimento));
    }

    @Transactional
    public Pedido actualizarEstado(Integer idPedido, String estadoSolicitado) {
        Preconditions.checkArgument(idPedido != null, "El pedido es obligatorio.");
        EstadoCocina destino = EstadoCocina.desdeSolicitud(estadoSolicitado);

        Pedido pedido = pedidoRepository.findActivoParaCocina(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));

        if (pedido.getFechaEnvioCocina() == null) {
            throw new ConflictException("El pedido aún no fue enviado a Cocina.");
        }

        EstadoCocina actual = EstadoCocina.desdePersistencia(
                pedido.getEstadoPedido() == null ? null : pedido.getEstadoPedido().getNombreEstado()
        );
        EstadoCocina esperado = actual.siguiente();
        if (esperado == null || esperado != destino) {
            throw new ConflictException(
                    "El pedido cambió de estado. Actualiza el tablero antes de continuar."
            );
        }

        if (pedido.getFechaConsumoInventario() == null) {
            consumoInventarioService.consumirParaPedido(pedido);
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_LIMA);
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

    private PedidoCocinaResponse presentar(Pedido pedido, EstimadorPreparacion estimador) {
        return PedidoCocinaResponse.from(pedido, estimador.estimar(cantidadPlatos(pedido)));
    }

    private EstimadorPreparacion construirEstimador(LocalDateTime ahora) {
        List<Pedido> historico = pedidoRepository.findPreparadosParaEstimacion(
                ahora.minusDays(DIAS_HISTORICO_ESTIMACION)
        );
        double totalMinutos = 0;
        double totalPlatos = 0;
        int muestras = 0;
        for (Pedido pedido : historico) {
            Long duracion = duracionPreparacion(pedido);
            int platos = cantidadPlatos(pedido);
            if (duracion == null || duracion < 1 || duracion > 180 || platos < 1) {
                continue;
            }
            totalMinutos += duracion;
            totalPlatos += platos;
            muestras++;
        }
        if (muestras == 0) {
            return new EstimadorPreparacion(MINUTOS_ESTIMADOS_POR_DEFECTO, 1);
        }
        return new EstimadorPreparacion(totalMinutos / muestras, totalPlatos / muestras);
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

    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
        }
        throw new IllegalStateException("No se pudo identificar al responsable de Cocina.");
    }

    private static int cantidadPlatos(Pedido pedido) {
        if (pedido.getDetalles() == null) {
            return 0;
        }
        return pedido.getDetalles().stream()
                .filter(detalle -> !Boolean.TRUE.equals(detalle.getEliminado()))
                .map(DetallePedido::getCantidad)
                .filter(cantidad -> cantidad != null && cantidad > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private static Long duracionPreparacion(Pedido pedido) {
        if (pedido.getFechaInicioPreparacion() == null || pedido.getFechaListo() == null
                || pedido.getFechaListo().isBefore(pedido.getFechaInicioPreparacion())) {
            return null;
        }
        return Duration.between(pedido.getFechaInicioPreparacion(), pedido.getFechaListo()).toMinutes();
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

    private record EstimadorPreparacion(double promedioMinutos, double promedioPlatos) {

        int estimar(int cantidadPlatos) {
            int platos = Math.max(1, cantidadPlatos);
            double factorCarga = Math.sqrt(platos / Math.max(1, promedioPlatos));
            return (int) Math.round(Math.max(5, Math.min(120, promedioMinutos * factorCarga)));
        }
    }

    private record TurnoCocina(String nombre, LocalDateTime desde, LocalDateTime hasta) {

        static TurnoCocina desdeReferencia(String referencia, LocalDateTime ahora) {
            TurnoCocina actual = actual(ahora);
            String valor = normalizar(referencia);
            Preconditions.checkArgument(
                    valor.isBlank() || "ACTUAL".equals(valor) || "ANTERIOR".equals(valor),
                    "El turno solicitado no es válido."
            );
            if (!"ANTERIOR".equals(valor)) {
                return actual;
            }
            LocalDateTime desdeAnterior = actual.desde().minusHours(8);
            return new TurnoCocina(
                    nombreParaHora(desdeAnterior.getHour()),
                    desdeAnterior,
                    actual.desde()
            );
        }

        static TurnoCocina actual(LocalDateTime ahora) {
            LocalDate fecha = ahora.toLocalDate();
            int hora = ahora.getHour();
            LocalDateTime desde;
            if (hora >= 6 && hora < 14) {
                desde = LocalDateTime.of(fecha, LocalTime.of(6, 0));
            } else if (hora >= 14 && hora < 22) {
                desde = LocalDateTime.of(fecha, LocalTime.of(14, 0));
            } else if (hora >= 22) {
                desde = LocalDateTime.of(fecha, LocalTime.of(22, 0));
            } else {
                desde = LocalDateTime.of(fecha.minusDays(1), LocalTime.of(22, 0));
            }
            return new TurnoCocina(nombreParaHora(desde.getHour()), desde, desde.plusHours(8));
        }

        private static String nombreParaHora(int hora) {
            return switch (hora) {
                case 6 -> "Mañana";
                case 14 -> "Tarde";
                default -> "Noche";
            };
        }
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
            return resolver(valor) == this;
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
    }
}
