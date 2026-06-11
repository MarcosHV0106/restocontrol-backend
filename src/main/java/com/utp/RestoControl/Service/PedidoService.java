package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.DetallePedidoRequest;
import com.utp.RestoControl.Dto.PedidoRequest;
import com.utp.RestoControl.Entity.*;

import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

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

        Usuario usuario = usuarioService.buscarPorId(
                request.getIdUsuario()
        );

        EstadoPedido estado = estadoPedidoService.buscarPorId(
                request.getIdEstadoPedido()
        );

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

        pedido.setIdUsuario(usuario);

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

        return pedidoRepository.save(
                pedido
        );
    }
}
