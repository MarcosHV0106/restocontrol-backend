package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.EstimacionDiariaItemRequest;
import com.utp.RestoControl.Dto.EstimacionDiariaResponse;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.EstimacionDiaria;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Entity.Usuario;
import com.utp.RestoControl.Exception.ConflictException;
import com.utp.RestoControl.Repository.AlimentoRepository;
import com.utp.RestoControl.Repository.DetallePedidoRepository;
import com.utp.RestoControl.Repository.EstimacionDiariaRepository;
import com.utp.RestoControl.Repository.UsuarioRepository;
import com.utp.RestoControl.Security.UserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstimacionDiariaService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final int MAXIMO_PORCIONES = 10_000;
    private static final int MAXIMO_DIAS_FUTURO = 365;

    private final EstimacionDiariaRepository estimacionRepository;
    private final AlimentoRepository alimentoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public EstimacionDiariaResponse consultar(LocalDate fecha) {
        validarFechaObligatoria(fecha);
        PlanGuardado plan = cargarPlanGuardado(fecha);
        return construirRespuesta(fecha, plan.porciones(), plan.datos());
    }

    @Transactional(readOnly = true)
    public EstimacionDiariaResponse validar(
            LocalDate fecha,
            List<EstimacionDiariaItemRequest> solicitudes
    ) {
        validarFechaGuardado(fecha);
        Map<Integer, Alimento> alimentos = cargarAlimentos();
        Map<Integer, Integer> porciones = validarSolicitudes(solicitudes, alimentos);
        return construirRespuesta(fecha, porciones, cargarPlanGuardado(fecha).datos());
    }

    @Transactional
    public EstimacionDiariaResponse guardar(
            LocalDate fecha,
            List<EstimacionDiariaItemRequest> solicitudes
    ) {
        validarFechaGuardado(fecha);
        Map<Integer, Alimento> alimentos = cargarAlimentos();
        Map<Integer, Integer> porcionesSolicitadas = validarSolicitudes(solicitudes, alimentos);
        EstimacionDiariaResponse validacion = construirRespuesta(
                fecha,
                porcionesSolicitadas,
                DatosGuardado.vacio()
        );
        if (!validacion.guardable()) {
            throw new ConflictException(
                    "La estimacion no puede guardarse: retira los platos sin receta o configura sus insumos."
            );
        }

        Map<Integer, EstimacionDiaria> existentes = estimacionRepository
                .findByFecha(fecha).stream()
                .collect(HashMap::new,
                        (mapa, estimacion) -> mapa.put(
                                estimacion.getAlimento().getIdAlimento(), estimacion),
                        HashMap::putAll);
        Usuario usuario = obtenerUsuarioActual();
        List<EstimacionDiaria> cambios = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entrada : porcionesSolicitadas.entrySet()) {
            Integer idAlimento = entrada.getKey();
            Integer porciones = entrada.getValue();
            EstimacionDiaria estimacion = existentes.remove(idAlimento);

            if (porciones == 0) {
                if (estimacion != null) {
                    estimacion.setEliminado(true);
                    estimacion.setPorciones(0);
                    estimacion.setUsuario(usuario);
                    cambios.add(estimacion);
                }
                continue;
            }

            if (estimacion == null) {
                estimacion = new EstimacionDiaria();
                estimacion.setFecha(fecha);
                estimacion.setAlimento(alimentos.get(idAlimento));
            }
            estimacion.setPorciones(porciones);
            estimacion.setUsuario(usuario);
            estimacion.setEliminado(false);
            cambios.add(estimacion);
        }

        existentes.values().forEach(estimacion -> {
            estimacion.setEliminado(true);
            estimacion.setPorciones(0);
            estimacion.setUsuario(usuario);
            cambios.add(estimacion);
        });

        estimacionRepository.saveAll(cambios);
        estimacionRepository.flush();
        boolean guardada = porcionesSolicitadas.values().stream().anyMatch(porciones -> porciones > 0);
        DatosGuardado datosGuardados = new DatosGuardado(
                guardada,
                guardada ? nombreCompleto(usuario) : null,
                guardada ? LocalDateTime.now(ZONA_LIMA) : null
        );
        return construirRespuesta(fecha, porcionesSolicitadas, datosGuardados);
    }

    private Map<Integer, Alimento> cargarAlimentos() {
        return alimentoRepository.findByEliminadoFalse().stream()
                .collect(LinkedHashMap::new,
                        (mapa, alimento) -> mapa.put(alimento.getIdAlimento(), alimento),
                        LinkedHashMap::putAll);
    }

    private Map<Integer, Integer> validarSolicitudes(
            List<EstimacionDiariaItemRequest> solicitudes,
            Map<Integer, Alimento> alimentos
    ) {
        Preconditions.checkArgument(solicitudes != null, "La estimacion es obligatoria.");
        Set<Integer> idsRecibidos = new HashSet<>();
        Map<Integer, Integer> porcionesSolicitadas = new LinkedHashMap<>();

        for (EstimacionDiariaItemRequest solicitud : solicitudes) {
            Preconditions.checkArgument(
                    solicitud != null && solicitud.idAlimento() != null,
                    "Cada fila debe indicar un alimento."
            );
            Preconditions.checkArgument(
                    idsRecibidos.add(solicitud.idAlimento()),
                    "No se puede repetir un alimento en la estimacion."
            );
            int porciones = solicitud.porciones() == null ? 0 : solicitud.porciones();
            Preconditions.checkArgument(
                    porciones >= 0 && porciones <= MAXIMO_PORCIONES,
                    "Las porciones deben estar entre 0 y 10000."
            );

            Alimento alimento = alimentos.get(solicitud.idAlimento());
            Preconditions.checkArgument(alimento != null, "El alimento indicado no existe.");
            Preconditions.checkArgument(
                    Boolean.TRUE.equals(alimento.getDisponible()),
                    "No se puede planificar un alimento no disponible."
            );
            porcionesSolicitadas.put(solicitud.idAlimento(), porciones);
        }
        return porcionesSolicitadas;
    }

    private PlanGuardado cargarPlanGuardado(LocalDate fecha) {
        List<EstimacionDiaria> estimaciones = estimacionRepository.findByFechaAndEliminadoFalse(fecha);
        Map<Integer, Integer> porciones = estimaciones.stream()
                .collect(HashMap::new,
                        (mapa, estimacion) -> mapa.put(
                                estimacion.getAlimento().getIdAlimento(), estimacion.getPorciones()),
                        HashMap::putAll);
        EstimacionDiaria ultima = estimaciones.stream()
                .max(Comparator.comparing(
                        EstimacionDiaria::getFechaActualizacion,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        DatosGuardado datos = ultima == null
                ? DatosGuardado.vacio()
                : new DatosGuardado(
                        true,
                        nombreCompleto(ultima.getUsuario()),
                        ultima.getFechaActualizacion()
                );
        return new PlanGuardado(porciones, datos);
    }

    private EstimacionDiariaResponse construirRespuesta(
            LocalDate fecha,
            Map<Integer, Integer> porciones,
            DatosGuardado datosGuardado
    ) {
        List<Alimento> alimentos = alimentoRepository.findByEliminadoFalse().stream()
                .filter(alimento -> Boolean.TRUE.equals(alimento.getDisponible()))
                .sorted(Comparator.comparing(Alimento::getNombreAlimento, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<Integer, Integer> porcionesProcesadas = cargarPorcionesProcesadas(fecha);
        Map<Integer, AcumuladoInsumo> requerimientos = new LinkedHashMap<>();
        Map<Integer, Boolean> recetaValida = new HashMap<>();
        Map<Integer, Set<Integer>> insumosPorAlimento = new HashMap<>();

        for (Alimento alimento : alimentos) {
            int cantidadPorciones = porciones.getOrDefault(alimento.getIdAlimento(), 0);
            int procesadas = porcionesProcesadas.getOrDefault(alimento.getIdAlimento(), 0);
            int pendientes = Math.max(cantidadPorciones - procesadas, 0);

            List<RecetaAlimento> receta = alimento.getReceta() == null
                    ? List.of()
                    : alimento.getReceta();
            boolean valida = !receta.isEmpty() && receta.stream().allMatch(this::ingredienteValido);
            recetaValida.put(alimento.getIdAlimento(), valida);
            if (pendientes <= 0 || !valida) {
                continue;
            }

            Set<Integer> idsInsumo = new HashSet<>();
            for (RecetaAlimento ingrediente : receta) {
                Insumo insumo = ingrediente.getInsumo();
                BigDecimal requerido = ingrediente.getCantidad()
                        .multiply(BigDecimal.valueOf(pendientes));
                AcumuladoInsumo acumulado = requerimientos.computeIfAbsent(
                        insumo.getIdInsumo(),
                        id -> new AcumuladoInsumo(insumo)
                );
                acumulado.cantidad = acumulado.cantidad.add(requerido);
                idsInsumo.add(insumo.getIdInsumo());
            }
            insumosPorAlimento.put(alimento.getIdAlimento(), idsInsumo);
        }

        Map<Integer, BigDecimal> faltantes = new HashMap<>();
        List<EstimacionDiariaResponse.InsumoRequerido> resumenInsumos = requerimientos.values()
                .stream()
                .sorted(Comparator.comparing(
                        acumulado -> acumulado.insumo.getNombreInsumo(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(acumulado -> {
                    BigDecimal stock = cantidad(acumulado.insumo.getStockActual());
                    BigDecimal requerido = cantidad(acumulado.cantidad);
                    BigDecimal faltante = requerido.subtract(stock).max(BigDecimal.ZERO);
                    faltantes.put(acumulado.insumo.getIdInsumo(), faltante);
                    return new EstimacionDiariaResponse.InsumoRequerido(
                            acumulado.insumo.getIdInsumo(),
                            acumulado.insumo.getNombreInsumo(),
                            acumulado.insumo.getUnidadMedida(),
                            stock,
                            requerido,
                            faltante
                    );
                })
                .toList();

        List<EstimacionDiariaResponse.PlatoEstimado> platos = alimentos.stream()
                .map(alimento -> construirPlato(
                        alimento,
                        porciones.getOrDefault(alimento.getIdAlimento(), 0),
                        porcionesProcesadas.getOrDefault(alimento.getIdAlimento(), 0),
                        recetaValida.getOrDefault(alimento.getIdAlimento(), false),
                        insumosPorAlimento.getOrDefault(alimento.getIdAlimento(), Set.of()),
                        faltantes
                ))
                .toList();
        int totalPorciones = platos.stream().mapToInt(EstimacionDiariaResponse.PlatoEstimado::porciones).sum();
        int totalProcesadas = platos.stream()
                .mapToInt(EstimacionDiariaResponse.PlatoEstimado::porcionesProcesadas)
                .sum();
        int totalPendientes = platos.stream()
                .mapToInt(EstimacionDiariaResponse.PlatoEstimado::porcionesPendientes)
                .sum();
        boolean guardable = platos.stream()
                .filter(plato -> plato.porciones() > 0)
                .allMatch(EstimacionDiariaResponse.PlatoEstimado::planificable);
        boolean factible = guardable && platos.stream()
                .filter(plato -> plato.porcionesPendientes() > 0)
                .allMatch(plato -> "SUFICIENTE".equals(plato.estado()));

        return new EstimacionDiariaResponse(
                fecha,
                datosGuardado.guardada(),
                guardable,
                factible,
                totalPorciones,
                totalProcesadas,
                totalPendientes,
                datosGuardado.responsable(),
                datosGuardado.fechaActualizacion(),
                platos,
                resumenInsumos
        );
    }

    private EstimacionDiariaResponse.PlatoEstimado construirPlato(
            Alimento alimento,
            int porciones,
            int porcionesProcesadas,
            boolean recetaValida,
            Set<Integer> idsInsumo,
            Map<Integer, BigDecimal> faltantes
    ) {
        int porcionesPendientes = Math.max(porciones - porcionesProcesadas, 0);
        String estado;
        String detalle;
        if (!recetaValida) {
            estado = "SIN_RECETA";
            detalle = "Plato heredado no planificable: configura una receta valida.";
        } else if (porciones <= 0 && porcionesProcesadas > 0) {
            estado = "SUPERADA";
            detalle = "Ya se procesaron %d porciones sin una meta vigente."
                    .formatted(porcionesProcesadas);
        } else if (porciones <= 0) {
            estado = "SIN_PLANIFICAR";
            detalle = "Define las porciones a preparar.";
        } else if (porcionesPendientes <= 0) {
            estado = porcionesProcesadas > porciones ? "SUPERADA" : "COMPLETADA";
            detalle = porcionesProcesadas > porciones
                    ? "La operacion supero la meta en %d porciones."
                            .formatted(porcionesProcesadas - porciones)
                    : "La meta diaria ya fue procesada por Cocina.";
        } else {
            List<String> insumosFaltantes = idsInsumo.stream()
                    .filter(id -> faltantes.getOrDefault(id, BigDecimal.ZERO)
                            .compareTo(BigDecimal.ZERO) > 0)
                    .map(id -> requerimientoFaltante(id, alimento, faltantes.get(id)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            if (insumosFaltantes.isEmpty()) {
                estado = "SUFICIENTE";
                detalle = porcionesProcesadas > 0
                        ? "Stock suficiente para %d pendientes; %d ya procesadas."
                                .formatted(porcionesPendientes, porcionesProcesadas)
                        : "Los insumos cubren la planificacion del dia.";
            } else {
                estado = "INSUFICIENTE";
                detalle = "Para las %d porciones pendientes falta %s."
                        .formatted(porcionesPendientes, String.join(", ", insumosFaltantes));
            }
        }

        return new EstimacionDiariaResponse.PlatoEstimado(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento(),
                alimento.getCategoria() == null ? "Sin categoria" : alimento.getCategoria().getNombreCategoria(),
                recetaValida,
                porciones,
                porcionesProcesadas,
                porcionesPendientes,
                estado,
                detalle
        );
    }

    private String requerimientoFaltante(
            Integer idInsumo,
            Alimento alimento,
            BigDecimal faltante
    ) {
        return alimento.getReceta().stream()
                .map(RecetaAlimento::getInsumo)
                .filter(insumo -> insumo.getIdInsumo().equals(idInsumo))
                .findFirst()
                .map(insumo -> "%s (%s %s)".formatted(
                        insumo.getNombreInsumo(),
                        cantidad(faltante).stripTrailingZeros().toPlainString(),
                        insumo.getUnidadMedida()))
                .orElse("un insumo");
    }

    private boolean ingredienteValido(RecetaAlimento ingrediente) {
        return ingrediente != null
                && ingrediente.getCantidad() != null
                && ingrediente.getCantidad().compareTo(BigDecimal.ZERO) > 0
                && ingrediente.getInsumo() != null
                && !Boolean.TRUE.equals(ingrediente.getInsumo().getEliminado());
    }

    private Map<Integer, Integer> cargarPorcionesProcesadas(LocalDate fecha) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hastaExclusiva = fecha.plusDays(1).atStartOfDay();
        Map<Integer, Integer> procesadas = new HashMap<>();
        detallePedidoRepository.sumarCantidadesProcesadas(desde, hastaExclusiva)
                .forEach(fila -> procesadas.put(
                        fila.getIdAlimento(),
                        enteroSeguro(fila.getCantidad())
                ));
        return procesadas;
    }

    private int enteroSeguro(Long valor) {
        if (valor == null || valor <= 0) {
            return 0;
        }
        return valor > Integer.MAX_VALUE ? Integer.MAX_VALUE : valor.intValue();
    }

    private String nombreCompleto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        String completo = (nombre + " " + apellido).trim();
        return completo.isBlank() ? null : completo;
    }

    private void validarFechaObligatoria(LocalDate fecha) {
        Preconditions.checkArgument(fecha != null, "La fecha de la estimacion es obligatoria.");
    }

    private void validarFechaGuardado(LocalDate fecha) {
        validarFechaObligatoria(fecha);
        LocalDate hoy = LocalDate.now(ZONA_LIMA);
        Preconditions.checkArgument(!fecha.isBefore(hoy), "No se puede modificar una estimacion pasada.");
        Preconditions.checkArgument(
                !fecha.isAfter(hoy.plusDays(MAXIMO_DIAS_FUTURO)),
                "La estimacion no puede superar un ano de anticipacion."
        );
    }

    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return usuarioRepository.findByIdUsuarioAndEliminadoFalse(principal.getId())
                    .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado."));
        }
        throw new AccessDeniedException("No se pudo identificar al usuario autenticado.");
    }

    private BigDecimal cantidad(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(4, RoundingMode.HALF_UP);
    }

    private static final class AcumuladoInsumo {
        private final Insumo insumo;
        private BigDecimal cantidad = BigDecimal.ZERO;

        private AcumuladoInsumo(Insumo insumo) {
            this.insumo = insumo;
        }
    }

    private record DatosGuardado(
            boolean guardada,
            String responsable,
            LocalDateTime fechaActualizacion
    ) {
        private static DatosGuardado vacio() {
            return new DatosGuardado(false, null, null);
        }
    }

    private record PlanGuardado(
            Map<Integer, Integer> porciones,
            DatosGuardado datos
    ) {
    }
}
