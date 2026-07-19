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
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.PedidoRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
public class PedidoService {

    private static final Integer ESTADO_MESA_LIBRE = 1;
    private static final Integer ESTADO_MESA_OCUPADA = 2;
    private static final Integer ESTADO_MESA_COBRAR = 4;
    private static final Integer ESTADO_PEDIDO_PAGADO = 4;
    private static final Set<String> ROLES_RESPONSABLES = Set.of("MESERO", "ADMIN", "ADMINISTRADOR");

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final MesaService mesaService;
    private final UsuarioService usuarioService;
    private final EstadoPedidoService estadoPedidoService;
    private final ModalidadPedidoService modalidadPedidoService;
    private final AlimentoService alimentoService;

    @Transactional
    public Pedido guardar(PedidoRequest request) {
        validarRequestBase(request);
        ModalidadPedido modalidad = modalidadPedidoService.buscarPorId(request.getIdModalidadPedido());
        TipoModalidad tipoModalidad = TipoModalidad.desde(modalidad);
        Mesa mesa = prepararMesaNueva(request, tipoModalidad);
        DatosCliente datosCliente = validarDatosCliente(request, tipoModalidad);

        Usuario usuario = usuarioService.buscarPorId(obtenerPrincipal().getId());
        EstadoPedido estado = estadoPedidoService.buscarPorId(1);

        Pedido pedido = new Pedido();
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setObservacion(normalizarObservacion(request.getObservacion()));
        pedido.setClienteNombre(datosCliente.nombre());
        pedido.setClienteTelefono(datosCliente.telefono());
        pedido.setDireccionEntrega(datosCliente.direccion());
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
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosSegunRol() {
        if (puedeGestionarPedidosDeTodos()) {
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

        List<Integer> ids = idsMesa.stream().filter(Objects::nonNull).distinct().toList();
        List<Pedido> pedidos = puedeGestionarPedidosDeTodos()
                ? pedidoRepository.findUltimosActivosPorMesas(ids, ESTADO_PEDIDO_PAGADO)
                : pedidoRepository.findUltimosActivosPorMesasDelUsuario(
                        ids,
                        ESTADO_PEDIDO_PAGADO,
                        obtenerPrincipal().getId()
                );

        return pedidos.stream()
                .filter(pedido -> pedido.getIdMesa() != null)
                .collect(Collectors.toMap(
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

        Pedido pedido = pedidoRepository.findActivosPorMesa(idMesa).stream().findFirst().orElse(null);
        if (pedido != null) {
            verificarAcceso(pedido);
        }
        return pedido;
    }

    @Transactional
    public Pedido actualizar(Integer idPedido, PedidoRequest request) {
        validarRequestBase(request);
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarEditable(pedido);

        ModalidadPedido modalidad = modalidadPedidoService.buscarPorId(request.getIdModalidadPedido());
        Preconditions.checkArgument(
                pedido.getModalidadPedido().getIdModalidadPedido().equals(modalidad.getIdModalidadPedido()),
                "La modalidad no puede cambiarse despues de crear el pedido."
        );

        TipoModalidad tipoModalidad = TipoModalidad.desde(modalidad);
        validarMesaSinCambio(pedido, request, tipoModalidad);
        DatosCliente datosCliente = validarDatosCliente(request, tipoModalidad);

        pedido.setObservacion(normalizarObservacion(request.getObservacion()));
        pedido.setClienteNombre(datosCliente.nombre());
        pedido.setClienteTelefono(datosCliente.telefono());
        pedido.setDireccionEntrega(datosCliente.direccion());

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
    public Pedido enviarACocina(Integer idPedido) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarEditable(pedido);
        Preconditions.checkState(
                pedido.getDetalles() != null
                && pedido.getDetalles().stream().anyMatch(detalle -> !Boolean.TRUE.equals(detalle.getEliminado())),
                "El pedido debe tener al menos un producto para enviarse a Cocina."
        );

        pedido.setFechaEnvioCocina(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido reabrir(Integer idPedido) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);

        Preconditions.checkState(pedido.getFechaEnvioCocina() != null, "El pedido ya esta en borrador.");
        Preconditions.checkState(pedido.getFechaSolicitudCuenta() == null, "La cuenta ya fue solicitada.");
        Preconditions.checkState(
                pedido.getFechaConsumoInventario() == null && pedido.getFechaInicioPreparacion() == null,
                "El pedido ya inicio su preparacion y no puede reabrirse."
        );
        Preconditions.checkState(
                "PENDIENTE".equals(normalizar(pedido.getEstadoPedido().getNombreEstado())),
                "Solo puede reabrirse un pedido aun no iniciado por Cocina."
        );

        pedido.setFechaEnvioCocina(null);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido actualizarObservacion(Integer idPedido, String observacion) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);
        Preconditions.checkState(pedido.getFechaSolicitudCuenta() == null, "La cuenta ya fue solicitada.");

        pedido.setObservacion(normalizarObservacion(observacion));
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido solicitarCuenta(Integer idPedido) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);
        Preconditions.checkState(pedido.getFechaEnvioCocina() != null, "Primero envia el pedido a Cocina.");
        Preconditions.checkState(pedido.getFechaSolicitudCuenta() == null, "La cuenta ya fue solicitada.");
        Preconditions.checkState(
                "ENTREGADO".equals(normalizar(pedido.getEstadoPedido().getNombreEstado())),
                "La cuenta puede solicitarse cuando Cocina marque el pedido como entregado."
        );

        pedido.setFechaSolicitudCuenta(LocalDateTime.now());
        if (pedido.getIdMesa() != null) {
            mesaService.actualizarEstado(pedido.getIdMesa().getIdMesa(), ESTADO_MESA_COBRAR);
        }
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido anular(Integer idPedido, String motivo) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);

        Preconditions.checkState(pedido.getFechaSolicitudCuenta() == null, "No se puede anular una cuenta solicitada.");
        Preconditions.checkState(
                pedido.getFechaConsumoInventario() == null && pedido.getFechaInicioPreparacion() == null,
                "El pedido ya inicio su preparacion. Anularlo requiere un ajuste de inventario."
        );

        String motivoNormalizado = normalizarTexto(
                motivo,
                200,
                true,
                "El motivo de anulacion es obligatorio."
        );
        pedido.setEstadoPedido(buscarEstadoPorNombre("CANCELADO"));
        pedido.setFechaCancelacion(LocalDateTime.now());
        pedido.setMotivoCancelacion(motivoNormalizado);
        if (pedido.getIdMesa() != null) {
            mesaService.liberar(pedido.getIdMesa().getIdMesa());
        }
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido cambiarMesa(Integer idPedido, Integer idMesaDestino) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);
        Preconditions.checkState(pedido.getFechaSolicitudCuenta() == null, "La cuenta ya fue solicitada.");
        Preconditions.checkState(
                TipoModalidad.desde(pedido.getModalidadPedido()) == TipoModalidad.MESA,
                "Solo los pedidos de salon pueden cambiar de mesa."
        );
        Preconditions.checkState(pedido.getIdMesa() != null, "El pedido no tiene una mesa asignada.");

        Mesa destino = mesaService.transferirPedido(
                pedido.getIdMesa().getIdMesa(),
                idMesaDestino,
                false
        );
        pedido.setIdMesa(destino);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido transferirResponsable(Integer idPedido, Integer idUsuarioDestino) {
        Pedido pedido = buscarPedidoParaGestion(idPedido);
        verificarAcceso(pedido);
        validarNoCerrado(pedido);
        Preconditions.checkArgument(idUsuarioDestino != null, "El nuevo responsable es obligatorio.");
        Preconditions.checkArgument(
                pedido.getUsuario() == null || !idUsuarioDestino.equals(pedido.getUsuario().getIdUsuario()),
                "Selecciona un responsable diferente."
        );

        Usuario destino = usuarioService.buscarPorId(idUsuarioDestino);
        String rolDestino = destino.getRol() == null ? "" : normalizar(destino.getRol().getNombreRol());
        Preconditions.checkArgument(
                ROLES_RESPONSABLES.contains(rolDestino),
                "El usuario seleccionado no puede atender pedidos."
        );
        Preconditions.checkArgument(Boolean.TRUE.equals(destino.getDisponible()), "El usuario seleccionado no esta disponible.");
        Preconditions.checkArgument(!Boolean.TRUE.equals(destino.getPendiente()), "La cuenta del usuario aun no esta activa.");

        pedido.setUsuario(destino);
        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarResponsables() {
        return usuarioService.listarResponsablesDePedidos();
    }

    private Pedido buscarPedidoActivo(Integer idPedido) {
        Preconditions.checkArgument(idPedido != null, "El pedido es obligatorio.");
        return pedidoRepository.findByIdPedidoAndEliminadoFalse(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private Pedido buscarPedidoParaGestion(Integer idPedido) {
        Preconditions.checkArgument(idPedido != null, "El pedido es obligatorio.");
        return pedidoRepository.findActivoParaGestion(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private void verificarAcceso(Pedido pedido) {
        if (puedeGestionarPedidosDeTodos()) {
            return;
        }
        Integer idUsuario = obtenerPrincipal().getId();
        Integer idResponsable = pedido.getUsuario() == null ? null : pedido.getUsuario().getIdUsuario();
        if (!idUsuario.equals(idResponsable)) {
            throw new AccessDeniedException("No tienes permiso para acceder a este pedido.");
        }
    }

    private boolean puedeGestionarPedidosDeTodos() {
        return tieneRol("ROLE_ADMIN") || tieneRol("ROLE_CAJERO");
    }

    private boolean tieneRol(String rol) {
        return obtenerAutenticacion().getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
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
        validarNoCerrado(pedido);
        if (pedido.getFechaEnvioCocina() != null) {
            throw new ConflictException("Reabre el pedido antes de modificar sus productos.");
        }
    }

    private void validarNoCerrado(Pedido pedido) {
        String estado = pedido.getEstadoPedido() == null
                ? ""
                : normalizar(pedido.getEstadoPedido().getNombreEstado());
        Preconditions.checkState(
                !Set.of("PAGADO", "COBRADO", "CANCELADO").contains(estado),
                "El pedido ya esta cerrado."
        );
    }

    private void validarRequestBase(PedidoRequest request) {
        Preconditions.checkArgument(request != null, "El pedido es obligatorio.");
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

    private Mesa prepararMesaNueva(PedidoRequest request, TipoModalidad modalidad) {
        if (modalidad != TipoModalidad.MESA) {
            Preconditions.checkArgument(request.getIdMesa() == null, "Esta modalidad no debe asociarse a una mesa.");
            return null;
        }

        Preconditions.checkArgument(request.getIdMesa() != null, "La mesa es obligatoria para pedidos en salon.");
        Mesa mesa = mesaService.ocuparParaPedido(request.getIdMesa());
        Preconditions.checkState(
                pedidoRepository.findActivosPorMesa(request.getIdMesa()).isEmpty(),
                "La mesa ya tiene un pedido activo."
        );
        return mesa;
    }

    private void validarMesaSinCambio(Pedido pedido, PedidoRequest request, TipoModalidad modalidad) {
        if (modalidad == TipoModalidad.MESA) {
            Preconditions.checkArgument(request.getIdMesa() != null, "La mesa es obligatoria para pedidos en salon.");
            Preconditions.checkArgument(
                    pedido.getIdMesa() != null && pedido.getIdMesa().getIdMesa().equals(request.getIdMesa()),
                    "Usa la accion Cambiar mesa para reasignar el pedido."
            );
            return;
        }
        Preconditions.checkArgument(request.getIdMesa() == null, "Esta modalidad no debe asociarse a una mesa.");
    }

    private DatosCliente validarDatosCliente(PedidoRequest request, TipoModalidad modalidad) {
        if (modalidad == TipoModalidad.MESA) {
            return new DatosCliente(null, null, null);
        }

        String nombre = normalizarTexto(
                request.getClienteNombre(),
                120,
                true,
                "El nombre del cliente es obligatorio."
        );
        String telefono = normalizarTexto(
                request.getClienteTelefono(),
                20,
                modalidad == TipoModalidad.DELIVERY,
                "El telefono del cliente es obligatorio para delivery."
        );
        if (telefono != null) {
            Preconditions.checkArgument(
                    telefono.matches("[0-9+() -]{6,20}"),
                    "El telefono del cliente no tiene un formato valido."
            );
        }
        String direccion = normalizarTexto(
                request.getDireccionEntrega(),
                250,
                modalidad == TipoModalidad.DELIVERY,
                "La direccion de entrega es obligatoria para delivery."
        );
        return new DatosCliente(nombre, telefono, direccion);
    }

    private String normalizarObservacion(String observacion) {
        return normalizarTexto(observacion, 250, false, "");
    }

    private String normalizarTexto(String valor, int maximo, boolean obligatorio, String mensajeObligatorio) {
        if (valor == null || valor.isBlank()) {
            Preconditions.checkArgument(!obligatorio, mensajeObligatorio);
            return null;
        }
        String normalizado = valor.trim();
        Preconditions.checkArgument(
                normalizado.length() <= maximo,
                "El texto no puede superar los " + maximo + " caracteres."
        );
        return normalizado;
    }

    private List<DetallePedido> construirDetalles(Pedido pedido, List<DetallePedidoRequest> solicitudes) {
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

    private EstadoPedido buscarEstadoPorNombre(String nombre) {
        String esperado = normalizar(nombre);
        return estadoPedidoService.listar().stream()
                .filter(estado -> esperado.equals(normalizar(estado.getNombreEstado())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el estado requerido para el pedido: " + nombre + "."
                ));
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

    private record DatosCliente(String nombre, String telefono, String direccion) {
    }

    private enum TipoModalidad {
        MESA,
        PARA_LLEVAR,
        DELIVERY;

        static TipoModalidad desde(ModalidadPedido modalidad) {
            Preconditions.checkArgument(modalidad != null, "La modalidad del pedido es obligatoria.");
            String nombre = normalizar(modalidad.getNombreModalidad());
            return switch (nombre) {
                case "MESA", "SALON", "EN_MESA" -> MESA;
                case "PARA_LLEVAR", "LLEVAR" -> PARA_LLEVAR;
                case "DELIVERY", "REPARTO" -> DELIVERY;
                default -> throw new IllegalArgumentException("La modalidad seleccionada no esta soportada.");
            };
        }
    }
}
