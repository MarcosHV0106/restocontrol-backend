package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.CobroRequest;
import com.utp.RestoControl.Dto.PagoCobroRequest;
import com.utp.RestoControl.Entity.Cobro;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Entity.PagoCobro;
import com.utp.RestoControl.Entity.Pedido;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.CobroRepository;
import com.utp.RestoControl.Repository.PagoCobroRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CobroService {

    private static final Integer ESTADO_PEDIDO_PAGADO = 4;
    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");
    private static final Set<String> METODOS_PERMITIDOS = Set.of(
            "EFECTIVO",
            "TARJETA",
            "YAPE",
            "PLIN",
            "TRANSFERENCIA"
    );
    private static final Set<String> COMPROBANTES_PERMITIDOS = Set.of("BOLETA", "FACTURA");

    private final CobroRepository cobroRepository;
    private final PagoCobroRepository pagoCobroRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioService usuarioService;
    private final EstadoPedidoService estadoPedidoService;
    private final MesaService mesaService;

    @Transactional(readOnly = true)
    public List<Pedido> listarPendientesSegunRol() {
        if (puedeCobrarPedidosDeTodos()) {
            return pedidoRepository.findPendientesDeCobroConRelaciones();
        }
        return pedidoRepository.findPendientesDeCobroPorUsuario(obtenerPrincipal().getId());
    }

    @Transactional(readOnly = true)
    public Pedido buscarPedidoParaCobro(Integer idPedido) {
        Pedido pedido = pedidoRepository.findByIdPedidoAndEliminadoFalse(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
        verificarAccesoAlPedido(pedido);
        validarPedidoPendiente(pedido);
        return pedido;
    }

    @Transactional(readOnly = true)
    public Cobro buscarPorId(Integer idCobro) {
        Cobro cobro = cobroRepository.findByIdCobroAndEliminadoFalse(idCobro)
                .orElseThrow(() -> new ResourceNotFoundException("Cobro no encontrado."));

        if (!puedeCobrarPedidosDeTodos()) {
            Integer idUsuario = obtenerPrincipal().getId();
            Integer idCreador = cobro.getPedido().getUsuario().getIdUsuario();
            Integer idCajero = cobro.getUsuarioCajero().getIdUsuario();
            if (!idUsuario.equals(idCreador) && !idUsuario.equals(idCajero)) {
                throw new AccessDeniedException("No tienes permiso para consultar este cobro.");
            }
        }
        return cobro;
    }

    @Transactional
    public Cobro procesarCobro(Integer idPedido, CobroRequest request) {
        validarRequest(request);

        Pedido pedido = pedidoRepository.findActivoParaCobro(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
        verificarAccesoAlPedido(pedido);
        validarPedidoPendiente(pedido);

        BigDecimal totalPedido = moneda(pedido.getTotal());
        Preconditions.checkState(
                totalPedido.compareTo(BigDecimal.ZERO) > 0,
                "El pedido no tiene un total valido para cobrar."
        );

        BigDecimal descuento = request.getDescuento() == null
                ? moneda(BigDecimal.ZERO)
                : moneda(request.getDescuento());
        Preconditions.checkArgument(
                descuento.compareTo(BigDecimal.ZERO) >= 0,
                "El descuento no puede ser negativo."
        );
        Preconditions.checkArgument(
                descuento.compareTo(totalPedido) < 0,
                "El descuento debe ser menor al total del pedido."
        );

        BigDecimal totalCobrar = moneda(totalPedido.subtract(descuento));
        List<DatosPago> datosPagos = normalizarPagos(request.getPagos());
        BigDecimal sumaPagos = datosPagos.stream()
                .map(DatosPago::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Preconditions.checkArgument(
                moneda(sumaPagos).compareTo(totalCobrar) == 0,
                "La suma de los pagos debe coincidir con el total a cobrar."
        );

        Usuario cajero = usuarioService.buscarPorId(obtenerPrincipal().getId());
        LocalDateTime fechaCobro = LocalDateTime.now();
        BigDecimal subtotal = totalCobrar.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = moneda(totalCobrar.subtract(subtotal));
        BigDecimal totalRecibido = datosPagos.stream()
                .map(DatosPago::montoRecibido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vuelto = datosPagos.stream()
                .map(DatosPago::vuelto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Cobro cobro = new Cobro();
        cobro.setPedido(pedido);
        cobro.setFechaCobro(fechaCobro);
        cobro.setSubtotal(subtotal);
        cobro.setIgv(igv);
        cobro.setDescuento(descuento);
        cobro.setTotalCobrado(totalCobrar);
        cobro.setTotalRecibido(moneda(totalRecibido));
        cobro.setVuelto(moneda(vuelto));
        cobro.setUsuarioCajero(cajero);
        cobro.setEliminado(false);
        cobro = cobroRepository.save(cobro);

        List<PagoCobro> pagos = construirPagos(cobro, datosPagos);
        pagos = pagoCobroRepository.saveAllAndFlush(pagos);
        for (PagoCobro pago : pagos) {
            pago.setNumeroComprobante(generarNumeroComprobante(pago));
        }
        pagos = pagoCobroRepository.saveAll(pagos);
        cobro.setPagos(pagos);

        EstadoPedido estadoPagado = estadoPedidoService.buscarPorId(ESTADO_PEDIDO_PAGADO);
        pedido.setEstadoPedido(estadoPagado);
        pedido.setFechaPago(fechaCobro);
        pedido.setMetodoPago(resolverMetodoPagoPedido(datosPagos));
        pedidoRepository.save(pedido);
        if (pedido.getIdMesa() != null) {
            mesaService.liberar(pedido.getIdMesa().getIdMesa());
        }

        return cobro;
    }

    private void validarRequest(CobroRequest request) {
        Preconditions.checkArgument(request != null, "Los datos del cobro son obligatorios.");
        Preconditions.checkArgument(
                request.getPagos() != null && !request.getPagos().isEmpty(),
                "Debe registrar al menos un pago."
        );
        Preconditions.checkArgument(
                request.getPagos().size() <= 20,
                "No se pueden registrar mas de 20 pagos por pedido."
        );
    }

    private List<DatosPago> normalizarPagos(List<PagoCobroRequest> solicitudes) {
        List<DatosPago> pagos = new ArrayList<>();
        for (int indice = 0; indice < solicitudes.size(); indice++) {
            PagoCobroRequest solicitud = solicitudes.get(indice);
            Preconditions.checkArgument(solicitud != null, "El pago es obligatorio.");

            String metodo = normalizarOpcion(solicitud.getMetodoPago(), "El metodo de pago es obligatorio.");
            Preconditions.checkArgument(
                    METODOS_PERMITIDOS.contains(metodo),
                    "El metodo de pago no es valido."
            );

            BigDecimal monto = moneda(solicitud.getMonto());
            Preconditions.checkArgument(
                    monto.compareTo(BigDecimal.ZERO) > 0,
                    "El monto de cada pago debe ser mayor a cero."
            );

            BigDecimal montoRecibido;
            BigDecimal vuelto;
            if ("EFECTIVO".equals(metodo)) {
                Preconditions.checkArgument(
                        solicitud.getMontoRecibido() != null,
                        "Debe indicar el monto recibido para pagos en efectivo."
                );
                montoRecibido = moneda(solicitud.getMontoRecibido());
                Preconditions.checkArgument(
                        montoRecibido.compareTo(monto) >= 0,
                        "El monto recibido no puede ser menor al monto en efectivo."
                );
                vuelto = moneda(montoRecibido.subtract(monto));
            } else {
                montoRecibido = monto;
                vuelto = moneda(BigDecimal.ZERO);
            }

            String tipoComprobante = normalizarOpcion(
                    solicitud.getTipoComprobante(),
                    "El tipo de comprobante es obligatorio."
            );
            Preconditions.checkArgument(
                    COMPROBANTES_PERMITIDOS.contains(tipoComprobante),
                    "El tipo de comprobante no es valido."
            );

            String documento = normalizarDocumento(solicitud.getDocumentoCliente());
            String razonSocial = normalizarTexto(solicitud.getRazonSocial(), 150, "La razon social");
            validarDatosComprobante(tipoComprobante, documento, razonSocial);

            pagos.add(new DatosPago(
                    indice + 1,
                    metodo,
                    monto,
                    montoRecibido,
                    vuelto,
                    tipoComprobante,
                    documento,
                    "FACTURA".equals(tipoComprobante) ? razonSocial : null,
                    normalizarTexto(solicitud.getReferencia(), 80, "La referencia")
            ));
        }
        return pagos;
    }

    private void validarDatosComprobante(
            String tipoComprobante,
            String documento,
            String razonSocial
    ) {
        if ("FACTURA".equals(tipoComprobante)) {
            Preconditions.checkArgument(
                    documento != null && documento.matches("\\d{11}"),
                    "La factura requiere un RUC de 11 digitos."
            );
            Preconditions.checkArgument(
                    razonSocial != null,
                    "La razon social es obligatoria para la factura."
            );
            return;
        }

        Preconditions.checkArgument(
                documento == null || documento.matches("\\d{8}"),
                "El DNI debe tener 8 digitos."
        );
    }

    private List<PagoCobro> construirPagos(Cobro cobro, List<DatosPago> datosPagos) {
        return datosPagos.stream().map(datos -> {
            PagoCobro pago = new PagoCobro();
            pago.setCobro(cobro);
            pago.setSecuencia(datos.secuencia());
            pago.setMetodoPago(datos.metodoPago());
            pago.setMonto(datos.monto());
            pago.setMontoRecibido(datos.montoRecibido());
            pago.setVuelto(datos.vuelto());
            pago.setTipoComprobante(datos.tipoComprobante());
            pago.setDocumentoCliente(datos.documentoCliente());
            pago.setRazonSocial(datos.razonSocial());
            pago.setReferencia(datos.referencia());
            pago.setEliminado(false);
            return pago;
        }).toList();
    }

    private String generarNumeroComprobante(PagoCobro pago) {
        String serie = "FACTURA".equals(pago.getTipoComprobante()) ? "F001" : "B001";
        return "%s-%08d".formatted(serie, pago.getIdPagoCobro());
    }

    private String resolverMetodoPagoPedido(List<DatosPago> pagos) {
        Set<String> metodos = pagos.stream()
                .map(DatosPago::metodoPago)
                .collect(Collectors.toSet());
        return metodos.size() == 1 ? metodos.iterator().next() : "MIXTO";
    }

    private void validarPedidoPendiente(Pedido pedido) {
        String estado = pedido.getEstadoPedido() == null
                || pedido.getEstadoPedido().getNombreEstado() == null
                ? ""
                : pedido.getEstadoPedido().getNombreEstado().trim().toUpperCase(Locale.ROOT);
        boolean cerrado = pedido.getEstadoPedido() != null
                && (ESTADO_PEDIDO_PAGADO.equals(pedido.getEstadoPedido().getIdEstadoPedido())
                || Set.of("PAGADO", "COBRADO", "CANCELADO").contains(estado));

        Preconditions.checkState(!cerrado, "El pedido ya esta cerrado y no puede cobrarse.");
        Preconditions.checkState(
                pedido.getFechaSolicitudCuenta() != null,
                "El pedido aun no fue enviado a Caja."
        );
        Preconditions.checkState(
                !cobroRepository.existsByPedido_IdPedidoAndEliminadoFalse(pedido.getIdPedido()),
                "El pedido ya tiene un cobro registrado."
        );
    }

    private void verificarAccesoAlPedido(Pedido pedido) {
        if (puedeCobrarPedidosDeTodos()) {
            return;
        }

        Integer idUsuario = obtenerPrincipal().getId();
        Integer idCreador = pedido.getUsuario() == null ? null : pedido.getUsuario().getIdUsuario();
        if (!idUsuario.equals(idCreador)) {
            throw new AccessDeniedException("No tienes permiso para cobrar este pedido.");
        }
    }

    private boolean puedeCobrarPedidosDeTodos() {
        Authentication authentication = obtenerAutenticacion();
        return authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_ADMIN".equals(authority.getAuthority())
                || "ROLE_CAJERO".equals(authority.getAuthority())
        );
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
            throw new AccessDeniedException("Debes iniciar sesion para acceder a caja.");
        }
        return authentication;
    }

    private BigDecimal moneda(BigDecimal valor) {
        Preconditions.checkArgument(valor != null, "El monto es obligatorio.");
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizarOpcion(String valor, String mensaje) {
        Preconditions.checkArgument(valor != null && !valor.isBlank(), mensaje);
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        return documento.replaceAll("\\s+", "").trim();
    }

    private String normalizarTexto(String valor, int maximo, String etiqueta) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim();
        Preconditions.checkArgument(
                normalizado.length() <= maximo,
                etiqueta + " no puede superar los " + maximo + " caracteres."
        );
        return normalizado;
    }

    private record DatosPago(
            Integer secuencia,
            String metodoPago,
            BigDecimal monto,
            BigDecimal montoRecibido,
            BigDecimal vuelto,
            String tipoComprobante,
            String documentoCliente,
            String razonSocial,
            String referencia
    ) {
    }
}
