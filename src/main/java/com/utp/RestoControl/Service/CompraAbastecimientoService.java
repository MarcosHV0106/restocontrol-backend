package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.CompraAbastecimientoRequest;
import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Entity.CompraAbastecimiento;
import com.utp.RestoControl.Entity.CompraAbastecimientoDetalle;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.Proveedor;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.CompraAbastecimientoDetalleRepository;
import com.utp.RestoControl.Repository.CompraAbastecimientoRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompraAbastecimientoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final CompraAbastecimientoRepository repository;
    private final CompraAbastecimientoDetalleRepository detalleRepository;
    private final ProveedorService proveedorService;
    private final InsumoRepository insumoRepository;
    private final LoteInsumoService loteService;
    private final UsuarioRepository usuarioRepository;
    private final AlertaInventarioService alertaService;

    @Transactional(readOnly = true)
    public List<CompraAbastecimiento> listar() {
        return repository.findByEliminadoFalseOrderByFechaCompraDescIdCompraDesc();
    }

    @Transactional(readOnly = true)
    public CompraAbastecimiento buscarPorId(Integer idCompra) {
        return repository.findByIdCompraAndEliminadoFalse(idCompra)
                .orElseThrow(() -> new ResourceNotFoundException("Compra de abastecimiento no encontrada."));
    }

    @Transactional
    public CompraAbastecimiento registrar(CompraAbastecimientoRequest request) {
        validarCabecera(request);
        Proveedor proveedor = proveedorService.buscarActivo(request.idProveedor());
        String documento = normalizarObligatorio(request.numeroDocumento(), 60,
                "El numero de documento es obligatorio.");
        if (repository.existsByProveedor_IdProveedorAndNumeroDocumentoIgnoreCaseAndEliminadoFalse(
                proveedor.getIdProveedor(), documento)) {
            throw new ConflictException("El documento ya fue registrado para este proveedor.");
        }

        CompraAbastecimiento compra = new CompraAbastecimiento();
        compra.setProveedor(proveedor);
        compra.setUsuarioAlmacenero(obtenerUsuarioActual());
        compra.setFechaCompra(request.fechaCompra());
        compra.setFechaRegistro(LocalDateTime.now(ZONA_LIMA));
        compra.setNumeroDocumento(documento);
        compra.setObservacion(normalizar(request.observacion(), 250));
        compra.setTotal(BigDecimal.ZERO);
        compra.setEliminado(false);
        compra = repository.save(compra);

        Set<Integer> insumosIncluidos = new HashSet<>();
        List<CompraAbastecimientoDetalle> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraAbastecimientoRequest.Detalle solicitud : request.detalles()) {
            validarDetalle(solicitud, insumosIncluidos);
            Insumo insumo = insumoRepository.findByIdInsumoAndEliminadoFalse(solicitud.idInsumo())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));

            LoteInsumoRequest loteRequest = new LoteInsumoRequest();
            loteRequest.setCantidad(solicitud.cantidad());
            loteRequest.setFechaVencimiento(solicitud.fechaVencimiento());
            loteRequest.setReferencia("COMPRA-" + documento);
            LoteInsumo lote = loteService.crear(insumo.getIdInsumo(), loteRequest);

            BigDecimal subtotal = solicitud.cantidad().multiply(solicitud.costoUnitario());
            CompraAbastecimientoDetalle detalle = new CompraAbastecimientoDetalle();
            detalle.setCompra(compra);
            detalle.setInsumo(insumo);
            detalle.setLote(lote);
            detalle.setCantidad(solicitud.cantidad());
            detalle.setCostoUnitario(solicitud.costoUnitario());
            detalle.setSubtotal(subtotal);
            detalles.add(detalle);
            total = total.add(subtotal);

            insumo.setCostoUnitario(solicitud.costoUnitario());
            insumoRepository.save(insumo);
        }

        detalleRepository.saveAll(detalles);
        compra.setDetalles(detalles);
        compra.setTotal(total);
        compra = repository.save(compra);
        alertaService.sincronizar();
        return compra;
    }

    private void validarCabecera(CompraAbastecimientoRequest request) {
        Preconditions.checkArgument(request != null, "Los datos de la compra son obligatorios.");
        Preconditions.checkArgument(request.idProveedor() != null, "El proveedor es obligatorio.");
        Preconditions.checkArgument(request.fechaCompra() != null, "La fecha de compra es obligatoria.");
        Preconditions.checkArgument(!request.fechaCompra().isAfter(LocalDate.now(ZONA_LIMA)),
                "La fecha de compra no puede estar en el futuro.");
        Preconditions.checkArgument(request.detalles() != null && !request.detalles().isEmpty(),
                "La compra debe incluir al menos un insumo.");
    }

    private void validarDetalle(CompraAbastecimientoRequest.Detalle detalle, Set<Integer> insumosIncluidos) {
        Preconditions.checkArgument(detalle != null && detalle.idInsumo() != null,
                "Cada detalle debe indicar un insumo.");
        Preconditions.checkArgument(insumosIncluidos.add(detalle.idInsumo()),
                "No se puede repetir un insumo en la misma compra.");
        Preconditions.checkArgument(detalle.cantidad() != null
                        && detalle.cantidad().compareTo(BigDecimal.ZERO) > 0,
                "La cantidad comprada debe ser mayor a cero.");
        Preconditions.checkArgument(detalle.costoUnitario() != null
                        && detalle.costoUnitario().compareTo(BigDecimal.ZERO) > 0,
                "El costo unitario debe ser mayor a cero.");
        Preconditions.checkArgument(detalle.fechaVencimiento() != null,
                "La fecha de vencimiento de cada lote es obligatoria.");
        Preconditions.checkArgument(!detalle.fechaVencimiento().isBefore(LocalDate.now(ZONA_LIMA)),
                "No se puede abastecer un lote vencido.");
    }

    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
        }
        throw new IllegalStateException("No se pudo identificar al responsable del abastecimiento.");
    }

    private String normalizarObligatorio(String valor, int maximo, String mensaje) {
        String texto = normalizar(valor, maximo);
        Preconditions.checkArgument(texto != null, mensaje);
        return texto;
    }

    private String normalizar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        Preconditions.checkArgument(texto.length() <= maximo, "El texto supera la longitud permitida.");
        return texto;
    }
}
