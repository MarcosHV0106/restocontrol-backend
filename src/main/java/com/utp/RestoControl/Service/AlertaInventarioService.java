package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.AtencionAlertaRequest;
import com.utp.RestoControl.Dto.ResumenAlertasResponse;
import com.utp.RestoControl.Dto.RetirarLoteRequest;
import com.utp.RestoControl.Entity.AlertaInventario;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.LoteInsumo;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.AlertaInventarioRepository;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Repository.LoteInsumoRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertaInventarioService {

    public static final String STOCK_BAJO = "STOCK_BAJO";
    public static final String VENCIMIENTO_PROXIMO = "VENCIMIENTO_PROXIMO";
    public static final String LOTE_VENCIDO = "LOTE_VENCIDO";
    private static final int DIAS_PROXIMO_VENCIMIENTO = 7;
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final AlertaInventarioRepository alertaRepository;
    private final InsumoRepository insumoRepository;
    private final LoteInsumoRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteInsumoService loteService;

    @Transactional
    public void sincronizar() {
        LocalDate hoy = LocalDate.now(ZONA_LIMA);
        resolverAlertasDeInsumosEliminados();
        for (Insumo insumo : insumoRepository.findByEliminadoFalse()) {
            boolean stockBajo = insumo.getStockActual() != null && insumo.getStockMinimo() != null
                    && insumo.getStockActual().compareTo(insumo.getStockMinimo()) <= 0;
            String clave = claveStock(insumo.getIdInsumo());
            if (stockBajo) {
                crearOActualizar(clave, STOCK_BAJO, insumo, null,
                        "Stock actual %.4f %s (minimo: %.4f %s)".formatted(
                                insumo.getStockActual(), insumo.getUnidadMedida(),
                                insumo.getStockMinimo(), insumo.getUnidadMedida()));
            } else {
                resolver(clave);
            }
        }

        for (LoteInsumo lote : loteRepository.findByEliminadoFalse()) {
            String claveProximo = claveLote(VENCIMIENTO_PROXIMO, lote.getIdLote());
            String claveVencido = claveLote(LOTE_VENCIDO, lote.getIdLote());
            if (Boolean.TRUE.equals(lote.getInsumo().getEliminado())
                    || lote.getCantidadActual() == null
                    || lote.getCantidadActual().compareTo(BigDecimal.ZERO) <= 0
                    || "RETIRADO".equals(lote.getEstado()) || lote.getFechaVencimiento() == null) {
                resolver(claveProximo);
                resolver(claveVencido);
                continue;
            }

            if (lote.getFechaVencimiento().isBefore(hoy)) {
                resolver(claveProximo);
                long dias = ChronoUnit.DAYS.between(lote.getFechaVencimiento(), hoy);
                crearOActualizar(claveVencido, LOTE_VENCIDO, lote.getInsumo(), lote,
                        "Lote %s vencido hace %d dia(s); existencia %.4f %s".formatted(
                                lote.getCodigo(), dias, lote.getCantidadActual(), lote.getInsumo().getUnidadMedida()));
            } else if (!lote.getFechaVencimiento().isAfter(hoy.plusDays(DIAS_PROXIMO_VENCIMIENTO))) {
                resolver(claveVencido);
                long dias = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());
                crearOActualizar(claveProximo, VENCIMIENTO_PROXIMO, lote.getInsumo(), lote,
                        "Lote %s vence en %d dia(s); existencia %.4f %s".formatted(
                                lote.getCodigo(), dias, lote.getCantidadActual(), lote.getInsumo().getUnidadMedida()));
            } else {
                resolver(claveProximo);
                resolver(claveVencido);
            }
        }
    }

    @Transactional
    public List<AlertaInventario> listar(String texto, String tipo, String estado) {
        sincronizar();
        String busqueda = normalizarFiltro(texto);
        String tipoFiltro = normalizarFiltro(tipo);
        String estadoFiltro = normalizarFiltro(estado);
        return alertaRepository.findByEliminadoFalseOrderByFechaGeneracionDesc().stream()
                .filter(alerta -> tipoFiltro == null || alerta.getTipo().equals(tipoFiltro))
                .filter(alerta -> estadoFiltro == null || alerta.getEstado().equals(estadoFiltro))
                .filter(alerta -> busqueda == null || coincide(alerta, busqueda))
                .toList();
    }

    @Transactional
    public ResumenAlertasResponse resumen() {
        sincronizar();
        return new ResumenAlertasResponse(
                alertaRepository.countByEstadoAndEliminadoFalse("ACTIVA"),
                alertaRepository.countByEstadoAndEliminadoFalse("REVISADA"),
                alertaRepository.countByEstadoAndEliminadoFalse("ATENDIDA")
        );
    }

    @Transactional
    public AlertaInventario atender(Integer idAlerta, AtencionAlertaRequest request) {
        Preconditions.checkArgument(request != null && request.getAccion() != null
                && !request.getAccion().isBlank(), "La accion es obligatoria.");
        AlertaInventario alerta = alertaRepository.findByIdAlertaAndEliminadoFalse(idAlerta)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada."));
        if ("ATENDIDA".equals(alerta.getEstado())) {
            throw new ConflictException("La alerta ya fue atendida.");
        }

        String accion = request.getAccion().trim().toUpperCase(Locale.ROOT);
        validarAccion(alerta, accion);
        alerta.setAccion(accion);
        alerta.setObservacion(normalizarObservacion(request.getObservacion()));
        alerta.setUsuarioAtencion(obtenerUsuarioActual());
        alerta.setFechaRevision(ahora());

        if ("RETIRAR_LOTE_MERMA".equals(accion)) {
            RetirarLoteRequest retiro = new RetirarLoteRequest();
            retiro.setMotivo(alerta.getObservacion() == null ? "Retiro por alerta de vencimiento" : alerta.getObservacion());
            retiro.setReferencia("ALERTA-" + alerta.getIdAlerta());
            loteService.retirar(alerta.getLote().getIdLote(), retiro);
            alerta.setEstado("ATENDIDA");
            alerta.setFechaAtencion(ahora());
            alerta.setClaveActiva(null);
        } else {
            alerta.setEstado("REVISADA");
        }
        return alertaRepository.save(alerta);
    }

    private void crearOActualizar(String clave, String tipo, Insumo insumo, LoteInsumo lote, String detalle) {
        AlertaInventario alerta = alertaRepository.findByClaveActivaAndEliminadoFalse(clave).orElse(null);
        if (alerta == null) {
            alerta = new AlertaInventario();
            alerta.setTipo(tipo);
            alerta.setEstado("ACTIVA");
            alerta.setInsumo(insumo);
            alerta.setLote(lote);
            alerta.setFechaGeneracion(ahora());
            alerta.setClaveActiva(clave);
            alerta.setEliminado(false);
        }
        alerta.setDetalle(detalle);
        alertaRepository.save(alerta);
    }

    private void resolver(String clave) {
        alertaRepository.findByClaveActivaAndEliminadoFalse(clave).ifPresent(alerta -> {
            resolver(alerta);
        });
    }

    private void resolverAlertasDeInsumosEliminados() {
        alertaRepository.findByEliminadoFalseOrderByFechaGeneracionDesc().stream()
                .filter(alerta -> alerta.getClaveActiva() != null)
                .filter(alerta -> Boolean.TRUE.equals(alerta.getInsumo().getEliminado()))
                .forEach(this::resolver);
    }

    private void resolver(AlertaInventario alerta) {
        alerta.setEstado("ATENDIDA");
        alerta.setFechaAtencion(ahora());
        alerta.setClaveActiva(null);
        alertaRepository.save(alerta);
    }

    private void validarAccion(AlertaInventario alerta, String accion) {
        if (STOCK_BAJO.equals(alerta.getTipo())) {
            Preconditions.checkArgument(
                    List.of("SOLICITAR_REPOSICION", "SOLO_SEGUIMIENTO").contains(accion),
                    "La accion no es valida para una alerta de stock bajo.");
            return;
        }
        Preconditions.checkArgument(
                List.of("RETIRAR_LOTE_MERMA", "SOLO_SEGUIMIENTO").contains(accion),
                "La accion no es valida para una alerta de vencimiento.");
        if (alerta.getLote() == null) {
            throw new ConflictException("La alerta no tiene un lote asociado.");
        }
    }

    private boolean coincide(AlertaInventario alerta, String busqueda) {
        return alerta.getInsumo().getNombreInsumo().toUpperCase(Locale.ROOT).contains(busqueda)
                || alerta.getDetalle().toUpperCase(Locale.ROOT).contains(busqueda)
                || (alerta.getLote() != null
                && alerta.getLote().getCodigo().toUpperCase(Locale.ROOT).contains(busqueda));
    }

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado."));
        }
        throw new IllegalStateException("No se pudo identificar al usuario autenticado.");
    }

    private String normalizarFiltro(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarObservacion(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        Preconditions.checkArgument(texto.length() <= 250, "La observacion no puede superar 250 caracteres.");
        return texto;
    }

    private String claveStock(Integer idInsumo) {
        return STOCK_BAJO + ":INSUMO:" + idInsumo;
    }

    private String claveLote(String tipo, Integer idLote) {
        return tipo + ":LOTE:" + idLote;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZONA_LIMA);
    }
}
