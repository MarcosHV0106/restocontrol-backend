package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.DetallePedidoRequest;
import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Dto.PedidoResponse;
import com.utp.RestoControl.Entity.*;
import com.utp.RestoControl.Exception.ResourceNotFoundException;

import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.utp.RestoControl.Security.UserPrincipal;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    private final DetallePedidoRepository detalleRepository;

    private final MesaService mesaService;

    private final UsuarioService usuarioService;

    private final EstadoPedidoService estadoPedidoService;

    private final ModalidadPedidoService modalidadPedidoService;

    private final AlimentoService alimentoService;

    private static final Integer ESTADO_LIBRE = 1;
    private static final Integer ESTADO_OCUPADA = 2;
    private static final Integer ESTADO_RESERVADA = 3;
    private static final Integer ESTADO_COBRAR = 4;

    @Transactional
    public Pedido guardar(
            PedidoRequest request) {

        Preconditions.checkArgument(
                request != null,
                "El pedido es obligatorio."
        );

        Preconditions.checkArgument(
                request.getDetalles() != null
                && !request.getDetalles().isEmpty(),
                "El pedido debe tener al menos un producto."
        );

        Mesa mesa = mesaService.buscarPorId(
                request.getIdMesa()
        );

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Usuario usuario = usuarioService.buscarPorId(principal.getId());

        EstadoPedido estado = estadoPedidoService.buscarPorId(1);

        ModalidadPedido modalidad = modalidadPedidoService.buscarPorId(
                request.getIdModalidadPedido()
        );

        Pedido pedido = new Pedido();

        pedido.setFechaPedido(
                LocalDateTime.now()
        );

        pedido.setObservacion(
                request.getObservacion()
        );

        pedido.setIdMesa(mesa);

        pedido.setUsuario(usuario);

        pedido.setEstadoPedido(estado);

        pedido.setModalidadPedido(modalidad);

        pedido.setEliminado(false);

        pedido.setTotal(BigDecimal.ZERO);

        pedido = pedidoRepository.save(pedido);

        BigDecimal totalPedido = BigDecimal.ZERO;

        List<DetallePedido> detalles
                = new ArrayList<>();

        for (DetallePedidoRequest detalleRequest
                : request.getDetalles()) {

            Preconditions.checkArgument(
                    detalleRequest.getCantidad() != null
                    && detalleRequest.getCantidad() > 0,
                    "La cantidad debe ser mayor a cero."
            );

            Preconditions.checkArgument(
                    detalleRequest.getIdAlimento() != null,
                    "El alimento es obligatorio."
            );

            Alimento alimento
                    = alimentoService.buscarPorId(
                            detalleRequest.getIdAlimento()
                    );

            BigDecimal precioUnitario
                    = alimento.getPrecio();

            BigDecimal subtotal
                    = precioUnitario.multiply(
                            BigDecimal.valueOf(
                                    detalleRequest.getCantidad()
                            )
                    );

            DetallePedido detalle
                    = new DetallePedido();

            detalle.setCantidad(
                    detalleRequest.getCantidad()
            );

            detalle.setPrecio_unitario(
                    precioUnitario
            );

            detalle.setSubtotal(
                    subtotal
            );

            detalle.setIdAlimento(
                    alimento
            );

            detalle.setIdPedido(
                    pedido
            );

            detalle.setEliminado(false);

            detalles.add(detalle);

            totalPedido
                    = totalPedido.add(
                            subtotal
                    );
        }

        detalleRepository.saveAll(
                detalles
        );

        pedido.setTotal(
                totalPedido
        );

        pedido = pedidoRepository.save(pedido);

        pedido.setDetalles(detalles);

        mesaService.actualizarEstado(
                mesa.getIdMesa(),
                ESTADO_OCUPADA
        );

        return pedido;

    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosSegunRol() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 1. Verificamos si es ADMIN
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return pedidoRepository.findActivosConRelaciones();
        } else {
            // 2. Aquí hacemos el cast seguro a UserPrincipal
            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            Integer idUsuarioLogueado = principal.getId();

            // 3. Filtramos por el ID del usuario logueado
            return pedidoRepository.findActivosConRelacionesByUsuario(idUsuarioLogueado);
        }
    }

    @Transactional(readOnly = true)
    public Map<Integer, Pedido> buscarUltimosPorMesas(Collection<Integer> idsMesa) {
        if (idsMesa == null || idsMesa.isEmpty()) {
            return Map.of();
        }

        return pedidoRepository.findUltimosActivosPorMesas(
                        idsMesa.stream().distinct().toList(),
                        ESTADO_COBRAR
                )
                .stream()
                .collect(Collectors.toMap(
                        pedido -> pedido.getIdMesa().getIdMesa(),
                        pedido -> pedido
                ));
    }

    @Transactional(readOnly = true)
    public Pedido buscarUltimoPorMesa(Integer idMesa) {

        Mesa mesa = mesaService.buscarPorId(idMesa);

        if (ESTADO_LIBRE.equals(
                mesa.getEstadoMesa().getIdEstadoMesa()
        )) {
            return null;
        }

        return pedidoRepository
                .findTopByIdMesa_IdMesaAndEstadoPedido_IdEstadoPedidoNotAndEliminadoFalseOrderByIdPedidoDesc(
                        idMesa,
                        ESTADO_COBRAR
                )
                .orElse(null);
    }

    @Transactional
    public Pedido cobrar(Integer idPedido) {

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(()
                        -> new ResourceNotFoundException(
                        "Pedido no encontrado."
                )
                );

        EstadoPedido estadoPagado
                = estadoPedidoService.buscarPorId(4);

        pedido.setEstadoPedido(
                estadoPagado
        );

        pedido.setFechaPago(LocalDateTime.now());

        if (pedido.getMetodoPago() == null || pedido.getMetodoPago().isBlank()) {
            pedido.setMetodoPago("NO_REGISTRADO");
        }

        Mesa mesa = pedido.getIdMesa();

        mesaService.actualizarEstado(
                mesa.getIdMesa(),
                ESTADO_COBRAR
        );

        return pedidoRepository.save(
                pedido
        );
    }
}
