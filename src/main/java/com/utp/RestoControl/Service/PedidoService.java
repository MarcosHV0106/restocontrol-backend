package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.DetallePedidoRequest;
import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.DetallePedido;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Entity.ModalidadPedido;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final Integer ESTADO_MESA_LIBRE = 1;
    private static final Integer ESTADO_MESA_OCUPADA = 2;
    private static final Integer ESTADO_PEDIDO_PAGADO = 4;

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final MesaService mesaService;
    private final UsuarioService usuarioService;
    private final EstadoPedidoService estadoPedidoService;
    private final ModalidadPedidoService modalidadPedidoService;
    private final AlimentoService alimentoService;

    @Transactional
    public Pedido guardar(PedidoRequest request) {
        validarRequest(request);

        Mesa mesa = mesaService.buscarPorId(request.getIdMesa());
        Usuario usuario = usuarioService.buscarPorId(obtenerPrincipal().getId());
        EstadoPedido estado = estadoPedidoService.buscarPorId(1);
        ModalidadPedido modalidad = modalidadPedidoService.buscarPorId(request.getIdModalidadPedido());

        Pedido pedido = new Pedido();
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setObservacion(normalizarObservacion(request.getObservacion()));
        pedido.setIdMesa(mesa);
        pedido.setUsuario(usuario);
        pedido.setEstadoPedido(estado);
        pedido.setModalidadPedido(modalidad);
        pedido.setEliminado(false);
        pedido.setTotal(BigDecimal.ZERO);
        pedido = pedidoRepository.save(pedido);

        List<DetallePedido> detalles = construirDetalles(pedido, request.getDetalles());
        detalleRepository.saveAll(detalles);

        pedido.setDetalles(detalles);
        pedido.setTotal(calcularTotal(detalles));
        pedido = pedidoRepository.save(pedido);

        mesaService.actualizarEstado(mesa.getIdMesa(), ESTADO_MESA_OCUPADA);
        return pedido;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosSegunRol() {
        if (esAdministrador()) {
            return pedidoRepository.findActivosConRelaciones();
        }

        return pedidoRepository.findActivosConRelacionesByUsuario(obtenerPrincipal().getId());
    }

    @Transactional(readOnly = true)
    public Pedido buscarPorIdSegunRol(Integer idPedido) {
        Pedido pedido = buscarPedidoActivo(idPedido);
        verificarAcceso(pedido);
        return pedido;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Pedido> buscarUltimosPorMesas(Collection<Integer> idsMesa) {
        if (idsMesa == null || idsMesa.isEmpty()) {
            return Map.of();
        }

        List<Integer> ids = idsMesa.stream().distinct().toList();
        List<Pedido> pedidos = esAdministrador()
                ? pedidoRepository.findUltimosActivosPorMesas(ids, ESTADO_PEDIDO_PAGADO)
                : pedidoRepository.findUltimosActivosPorMesasDelUsuario(
                        ids,
                        ESTADO_PEDIDO_PAGADO,
                        obtenerPrincipal().getId()
                );

        return pedidos.stream().collect(Collectors.toMap(
                pedido -> pedido.getIdMesa().getIdMesa(),
                pedido -> pedido
        ));
    }

    @Transactional(readOnly = true)
    public Pedido buscarUltimoPorMesa(Integer idMesa) {
        Mesa mesa = mesaService.buscarPorId(idMesa);
        if (ESTADO_MESA_LIBRE.equals(mesa.getEstadoMesa().getIdEstadoMesa())) {
            return null;
        }

        Pedido pedido = pedidoRepository
                .findTopByIdMesa_IdMesaAndEstadoPedido_IdEstadoPedidoNotAndEliminadoFalseOrderByIdPedidoDesc(
                        idMesa,
                        ESTADO_PEDIDO_PAGADO
                )
                .orElse(null);

        if (pedido != null) {
            verificarAcceso(pedido);
        }
        return pedido;
    }

    @Transactional
    public Pedido actualizar(Integer idPedido, PedidoRequest request) {
        validarRequest(request);

        Pedido pedido = buscarPedidoActivo(idPedido);
        verificarAcceso(pedido);
        validarEditable(pedido);

        Preconditions.checkArgument(
                pedido.getIdMesa().getIdMesa().equals(request.getIdMesa()),
                "No se puede cambiar la mesa de un pedido existente."
        );

        ModalidadPedido modalidad = modalidadPedidoService.buscarPorId(request.getIdModalidadPedido());
        pedido.setModalidadPedido(modalidad);
        pedido.setObservacion(normalizarObservacion(request.getObservacion()));

        List<DetallePedido> detallesAnteriores = detalleRepository
                .findByIdPedido_IdPedidoAndEliminadoFalse(idPedido);
        detallesAnteriores.forEach(detalle -> detalle.setEliminado(true));
        detalleRepository.saveAll(detallesAnteriores);

        List<DetallePedido> detallesNuevos = construirDetalles(pedido, request.getDetalles());
        detalleRepository.saveAll(detallesNuevos);

        pedido.setDetalles(detallesNuevos);
        pedido.setTotal(calcularTotal(detallesNuevos));
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cobrar(Integer idPedido) {
        Pedido pedido = buscarPedidoActivo(idPedido);
        verificarAcceso(pedido);
        validarEditable(pedido);

        pedido.setEstadoPedido(estadoPedidoService.buscarPorId(ESTADO_PEDIDO_PAGADO));
        pedido.setFechaPago(LocalDateTime.now());
        if (pedido.getMetodoPago() == null || pedido.getMetodoPago().isBlank()) {
            pedido.setMetodoPago("NO_REGISTRADO");
        }

        mesaService.actualizarEstado(pedido.getIdMesa().getIdMesa(), ESTADO_MESA_LIBRE);
        return pedidoRepository.save(pedido);
    }

    private Pedido buscarPedidoActivo(Integer idPedido) {
        Preconditions.checkArgument(idPedido != null, "El pedido es obligatorio.");
        return pedidoRepository.findByIdPedidoAndEliminadoFalse(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private void verificarAcceso(Pedido pedido) {
        if (esAdministrador()) {
            return;
        }

        Integer idUsuario = obtenerPrincipal().getId();
        Integer idCreador = pedido.getUsuario() == null ? null : pedido.getUsuario().getIdUsuario();
        if (!idUsuario.equals(idCreador)) {
            throw new AccessDeniedException("No tienes permiso para acceder a este pedido.");
        }
    }

    private boolean esAdministrador() {
        Authentication authentication = obtenerAutenticacion();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private UserPrincipal obtenerPrincipal() {
        Object principal = obtenerAutenticacion().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new AccessDeniedException("No se pudo identificar al usuario autenticado.");
    }

    private Authentication obtenerAutenticacion() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Debes iniciar sesion para acceder a los pedidos.");
        }
        return authentication;
    }

    private void validarEditable(Pedido pedido) {
        EstadoPedido estado = pedido.getEstadoPedido();
        String nombreEstado = estado == null || estado.getNombreEstado() == null
                ? ""
                : estado.getNombreEstado().trim();
        boolean pagado = estado != null
                && (ESTADO_PEDIDO_PAGADO.equals(estado.getIdEstadoPedido())
                || "PAGADO".equalsIgnoreCase(nombreEstado)
                || "COBRADO".equalsIgnoreCase(nombreEstado));

        Preconditions.checkState(!pagado, "No se puede modificar un pedido pagado.");
    }

    private void validarRequest(PedidoRequest request) {
        Preconditions.checkArgument(request != null, "El pedido es obligatorio.");
        Preconditions.checkArgument(request.getIdMesa() != null, "La mesa es obligatoria.");
        Preconditions.checkArgument(
                request.getIdModalidadPedido() != null,
                "La modalidad del pedido es obligatoria."
        );
        Preconditions.checkArgument(
                request.getDetalles() != null && !request.getDetalles().isEmpty(),
                "El pedido debe tener al menos un producto."
        );

        for (DetallePedidoRequest detalle : request.getDetalles()) {
            Preconditions.checkArgument(detalle != null, "El detalle del pedido es obligatorio.");
            Preconditions.checkArgument(detalle.getIdAlimento() != null, "El alimento es obligatorio.");
            Preconditions.checkArgument(
                    detalle.getCantidad() != null && detalle.getCantidad() > 0,
                    "La cantidad debe ser mayor a cero."
            );
        }
    }

    private String normalizarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            return null;
        }

        String observacionNormalizada = observacion.trim();
        Preconditions.checkArgument(
                observacionNormalizada.length() <= 250,
                "La observacion no puede superar los 250 caracteres."
        );
        return observacionNormalizada;
    }

    private List<DetallePedido> construirDetalles(
            Pedido pedido,
            List<DetallePedidoRequest> solicitudes
    ) {
        List<DetallePedido> detalles = new ArrayList<>();
        for (DetallePedidoRequest solicitud : solicitudes) {
            Alimento alimento = alimentoService.buscarPorId(solicitud.getIdAlimento());
            BigDecimal precioUnitario = alimento.getPrecio();
            Preconditions.checkState(precioUnitario != null, "El alimento no tiene un precio configurado.");

            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(solicitud.getCantidad()));
            DetallePedido detalle = new DetallePedido();
            detalle.setCantidad(solicitud.getCantidad());
            detalle.setPrecio_unitario(precioUnitario);
            detalle.setSubtotal(subtotal);
            detalle.setIdAlimento(alimento);
            detalle.setIdPedido(pedido);
            detalle.setEliminado(false);
            detalles.add(detalle);
        }
        return detalles;
    }

    private BigDecimal calcularTotal(List<DetallePedido> detalles) {
        return detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
