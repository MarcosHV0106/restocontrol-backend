package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.RecetaAlimento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlimentoResponse {

    private Integer idAlimento;
    private String nombreAlimento;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal costoReceta;
    private Boolean disponible;
    private Boolean bloqueadoCocina;
    private String motivoBloqueoCocina;
    private LocalDateTime fechaBloqueoCocina;
    private String responsableBloqueoCocina;
    private Boolean recetaConfigurada;
    private Boolean inventarioSuficiente;
    private Boolean disponibleParaPedidos;
    private Integer porcionesDisponibles;
    private String motivoNoDisponible;
    private CategoriaResponse categoria;

    public static AlimentoResponse from(Alimento alimento) {
        EstadoOperativo estado = calcularEstadoOperativo(alimento);
        return new AlimentoResponse(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento(),
                alimento.getDescripcion(),
                alimento.getPrecio(),
                calcularCostoReceta(alimento),
                alimento.getDisponible(),
                Boolean.TRUE.equals(alimento.getBloqueadoCocina()),
                alimento.getMotivoBloqueoCocina(),
                alimento.getFechaBloqueoCocina(),
                nombreResponsableCocina(alimento),
                estado.recetaConfigurada(),
                estado.inventarioSuficiente(),
                estado.disponibleParaPedidos(),
                estado.porcionesDisponibles(),
                estado.motivoNoDisponible(),
                CategoriaResponse.from(alimento.getCategoria())
        );
    }

    private static EstadoOperativo calcularEstadoOperativo(Alimento alimento) {
        List<RecetaAlimento> receta = alimento.getReceta() == null ? List.of() : alimento.getReceta();
        if (!Boolean.TRUE.equals(alimento.getDisponible())) {
            return new EstadoOperativo(!receta.isEmpty(), false, false, 0, "Deshabilitado en el menu");
        }
        if (receta.isEmpty()) {
            return new EstadoOperativo(false, false, false, 0, "Receta pendiente de configuracion");
        }
        boolean recetaValida = receta.stream().allMatch(detalle -> detalle.getCantidad() != null
                && detalle.getCantidad().compareTo(BigDecimal.ZERO) > 0
                && detalle.getInsumo() != null
                && !Boolean.TRUE.equals(detalle.getInsumo().getEliminado()));
        if (!recetaValida) {
            return new EstadoOperativo(false, false, false, 0, "La receta contiene insumos incompletos");
        }

        int porciones = receta.stream().mapToInt(detalle -> {
            BigDecimal stock = detalle.getInsumo().getStockActual() == null
                    ? BigDecimal.ZERO : detalle.getInsumo().getStockActual();
            BigDecimal posibles = stock.divide(detalle.getCantidad(), 0, RoundingMode.DOWN);
            return posibles.min(BigDecimal.valueOf(100000)).intValue();
        }).min().orElse(0);
        boolean inventarioSuficiente = porciones > 0;
        if (Boolean.TRUE.equals(alimento.getBloqueadoCocina())) {
            String motivoCocina = alimento.getMotivoBloqueoCocina();
            String motivo = motivoCocina == null || motivoCocina.isBlank()
                    ? "Cocina notifico que el producto esta agotado"
                    : "Cocina notifico: " + motivoCocina.trim();
            return new EstadoOperativo(true, inventarioSuficiente, false, porciones, motivo);
        }
        String motivo = inventarioSuficiente ? null : "Inventario insuficiente para una porcion";
        return new EstadoOperativo(true, inventarioSuficiente, inventarioSuficiente, porciones, motivo);
    }

    private static String nombreResponsableCocina(Alimento alimento) {
        if (alimento.getUsuarioBloqueoCocina() == null) {
            return null;
        }
        String nombre = (alimento.getUsuarioBloqueoCocina().getNombre() + " "
                + alimento.getUsuarioBloqueoCocina().getApellido()).trim();
        return nombre.isBlank() ? alimento.getUsuarioBloqueoCocina().getCorreo() : nombre;
    }

    private static BigDecimal calcularCostoReceta(Alimento alimento) {
        if (alimento.getReceta() == null) {
            return BigDecimal.ZERO;
        }

        return alimento.getReceta().stream()
                .filter(detalle -> detalle.getCantidad() != null
                        && detalle.getInsumo() != null
                        && detalle.getInsumo().getCostoUnitario() != null)
                .map(detalle -> detalle.getCantidad().multiply(detalle.getInsumo().getCostoUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record EstadoOperativo(
            boolean recetaConfigurada,
            boolean inventarioSuficiente,
            boolean disponibleParaPedidos,
            int porcionesDisponibles,
            String motivoNoDisponible
    ) {
    }
}
