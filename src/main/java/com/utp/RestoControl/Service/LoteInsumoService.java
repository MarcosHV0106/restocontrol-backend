package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.ActualizarLoteRequest;
import com.utp.RestoControl.Dto.AjusteLoteRequest;
import com.utp.RestoControl.Dto.LoteInsumoRequest;
import com.utp.RestoControl.Dto.RetirarLoteRequest;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.MovimientoInventario;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.MovimientoInventarioRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoteInsumoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final Set<String> TIPOS_AJUSTE = Set.of(
            "SALIDA", "MERMA", "CORRECCION_POSITIVA", "CORRECCION_NEGATIVA");

    private final LoteInsumoRepository loteRepository;
    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<LoteInsumo> listarPorInsumo(Integer idInsumo) {
        buscarInsumo(idInsumo);
        return loteRepository.findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(idInsumo);
    }

    @Transactional
    public LoteInsumo crear(Integer idInsumo, LoteInsumoRequest request) {
        Preconditions.checkArgument(request != null, "Los datos del lote son obligatorios.");
        Preconditions.checkArgument(request.getCantidad() != null
                && request.getCantidad().compareTo(BigDecimal.ZERO) > 0,
                "La cantidad del lote debe ser mayor a cero.");
        Preconditions.checkArgument(request.getFechaVencimiento() != null,
                "La fecha de vencimiento del lote es obligatoria.");
        Preconditions.checkArgument(!request.getFechaVencimiento().isBefore(LocalDate.now(ZONA_LIMA)),
                "No se puede registrar un lote vencido.");

        Insumo insumo = buscarInsumo(idInsumo);
        LoteInsumo lote = new LoteInsumo();
        lote.setInsumo(insumo);
        lote.setCodigo(generarCodigo(idInsumo));
        lote.setCantidadInicial(request.getCantidad());
        lote.setCantidadActual(request.getCantidad());
        lote.setFechaIngreso(LocalDate.now(ZONA_LIMA));
        lote.setFechaVencimiento(request.getFechaVencimiento());
        lote.setEstado("ACTIVO");
        lote.setEliminado(false);
        lote = loteRepository.save(lote);

        registrarMovimiento(lote, "INGRESO", request.getCantidad(), "Registro de lote",
                normalizar(request.getReferencia(), 100));
        recalcularStock(insumo);
        return lote;
    }

    @Transactional
    public LoteInsumo actualizar(Integer idLote, ActualizarLoteRequest request) {
        Preconditions.checkArgument(request != null, "Los datos del lote son obligatorios.");
        Preconditions.checkArgument(request.getFechaVencimiento() != null,
                "La fecha de vencimiento es obligatoria.");
        LoteInsumo lote = buscarParaActualizar(idLote);
        if ("RETIRADO".equals(lote.getEstado())) {
            throw new ConflictException("No se puede editar un lote retirado.");
        }

        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            String codigo = request.getCodigo().trim().toUpperCase(Locale.ROOT);
            Preconditions.checkArgument(codigo.length() <= 60, "El codigo no puede superar 60 caracteres.");
            if (!codigo.equalsIgnoreCase(lote.getCodigo()) && loteRepository.existsByCodigoIgnoreCase(codigo)) {
                throw new ConflictException("Ya existe un lote con ese codigo.");
            }
            lote.setCodigo(codigo);
        }
        lote.setFechaVencimiento(request.getFechaVencimiento());
        return loteRepository.save(lote);
    }

    @Transactional
    public LoteInsumo retirar(Integer idLote, RetirarLoteRequest request) {
        LoteInsumo lote = buscarParaActualizar(idLote);
        if (lote.getCantidadActual() == null
                || lote.getCantidadActual().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("El lote ya no tiene existencias.");
        }

        BigDecimal cantidad = lote.getCantidadActual();
        lote.setCantidadActual(BigDecimal.ZERO);
        lote.setEstado("RETIRADO");
        loteRepository.save(lote);

        String motivo = request == null ? null : normalizar(request.getMotivo(), 150);
        String referencia = request == null ? null : normalizar(request.getReferencia(), 100);
        registrarMovimiento(lote, "MERMA", cantidad,
                motivo == null ? "Retiro total del lote" : motivo, referencia);
        recalcularStock(lote.getInsumo());
        return lote;
    }

    @Transactional
    public LoteInsumo ajustar(Integer idLote, AjusteLoteRequest request) {
        Preconditions.checkArgument(request != null, "Los datos del ajuste son obligatorios.");
        String tipo = request.getTipo() == null ? "" : request.getTipo().trim().toUpperCase(Locale.ROOT);
        Preconditions.checkArgument(TIPOS_AJUSTE.contains(tipo), "El tipo de ajuste no es valido.");
        Preconditions.checkArgument(request.getCantidad() != null
                        && request.getCantidad().compareTo(BigDecimal.ZERO) > 0,
                "La cantidad del ajuste debe ser mayor a cero.");
        String motivo = normalizar(request.getMotivo(), 150);
        Preconditions.checkArgument(motivo != null, "El motivo del ajuste es obligatorio.");

        LoteInsumo lote = buscarParaActualizar(idLote);
        if ("RETIRADO".equals(lote.getEstado())) {
            throw new ConflictException("No se puede ajustar un lote retirado.");
        }

        boolean positivo = "CORRECCION_POSITIVA".equals(tipo);
        if (positivo) {
            if (lote.getFechaVencimiento() != null
                    && lote.getFechaVencimiento().isBefore(LocalDate.now(ZONA_LIMA))) {
                throw new ConflictException("No se puede aumentar la existencia de un lote vencido.");
            }
            lote.setCantidadActual(lote.getCantidadActual().add(request.getCantidad()));
            lote.setEstado("ACTIVO");
        } else {
            if (lote.getCantidadActual() == null
                    || lote.getCantidadActual().compareTo(request.getCantidad()) < 0) {
                throw new ConflictException("La cantidad supera la existencia disponible del lote.");
            }
            lote.setCantidadActual(lote.getCantidadActual().subtract(request.getCantidad()));
            if (lote.getCantidadActual().compareTo(BigDecimal.ZERO) == 0) {
                lote.setEstado("AGOTADO");
            }
        }

        loteRepository.save(lote);
        registrarMovimiento(lote, tipo, request.getCantidad(), motivo,
                normalizar(request.getReferencia(), 100));
        recalcularStock(lote.getInsumo());
        return lote;
    }

    @Transactional(readOnly = true)
    public LoteInsumo buscarPorId(Integer idLote) {
        return loteRepository.findByIdLoteAndEliminadoFalse(idLote)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado."));
    }

    private LoteInsumo buscarParaActualizar(Integer idLote) {
        return loteRepository.findParaActualizar(idLote)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado."));
    }

    private Insumo buscarInsumo(Integer idInsumo) {
        return insumoRepository.findByIdInsumoAndEliminadoFalse(idInsumo)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));
    }

    private void recalcularStock(Insumo insumo) {
        BigDecimal total = loteRepository
                .findByInsumo_IdInsumoAndEliminadoFalseOrderByFechaVencimientoAsc(insumo.getIdInsumo())
                .stream()
                .filter(lote -> "ACTIVO".equals(lote.getEstado()))
                .filter(lote -> lote.getFechaVencimiento() == null
                        || !lote.getFechaVencimiento().isBefore(LocalDate.now(ZONA_LIMA)))
                .map(LoteInsumo::getCantidadActual)
                .filter(cantidad -> cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        insumo.setStockActual(total);
        insumoRepository.save(insumo);
    }

    private void registrarMovimiento(
            LoteInsumo lote, String tipo, BigDecimal cantidad, String motivo, String referencia) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo(motivo);
        movimiento.setFechaMovimiento(LocalDateTime.now(ZONA_LIMA));
        movimiento.setReferencia(referencia);
        movimiento.setInsumo(lote.getInsumo());
        movimiento.setLote(lote);
        movimiento.setUsuario(obtenerUsuarioActual());
        movimiento.setEliminado(false);
        movimientoRepository.save(movimiento);
    }

    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId()).orElse(null);
        }
        return null;
    }

    private String generarCodigo(Integer idInsumo) {
        String fecha = LocalDateTime.now(ZONA_LIMA).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sufijo = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
        return "LOT-%d-%s-%s".formatted(idInsumo, fecha, sufijo);
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
