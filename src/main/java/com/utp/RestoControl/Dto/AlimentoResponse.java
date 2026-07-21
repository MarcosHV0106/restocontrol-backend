package com.utp.RestoControl.Dto;

import com.utp.RestoControl.Entity.Alimento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlimentoResponse {

    private Integer idAlimento;
    private String nombreAlimento;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
    private Boolean bloqueadoCocina;
    private String motivoBloqueoCocina;
    private LocalDateTime fechaBloqueoCocina;
    private String responsableBloqueoCocina;
    private Boolean disponibleParaPedidos;
    private String motivoNoDisponible;
    private CategoriaResponse categoria;

    public static AlimentoResponse from(Alimento alimento) {
        int stock = Math.max(0, alimento.getStock() == null ? 0 : alimento.getStock());
        boolean habilitado = Boolean.TRUE.equals(alimento.getDisponible());
        boolean bloqueado = Boolean.TRUE.equals(alimento.getBloqueadoCocina());
        boolean disponibleParaPedidos = habilitado && !bloqueado && stock > 0;

        return new AlimentoResponse(
                alimento.getIdAlimento(),
                alimento.getNombreAlimento(),
                alimento.getDescripcion(),
                alimento.getPrecio(),
                stock,
                habilitado,
                bloqueado,
                alimento.getMotivoBloqueoCocina(),
                alimento.getFechaBloqueoCocina(),
                nombreResponsableCocina(alimento),
                disponibleParaPedidos,
                motivoNoDisponible(alimento, stock),
                CategoriaResponse.from(alimento.getCategoria())
        );
    }

    private static String motivoNoDisponible(Alimento alimento, int stock) {
        if (!Boolean.TRUE.equals(alimento.getDisponible())) {
            return "Deshabilitado en el menu";
        }
        if (Boolean.TRUE.equals(alimento.getBloqueadoCocina())) {
            String motivo = alimento.getMotivoBloqueoCocina();
            return motivo == null || motivo.isBlank()
                    ? "No disponible temporalmente"
                    : motivo.trim();
        }
        return stock <= 0 ? "Sin stock disponible" : null;
    }

    private static String nombreResponsableCocina(Alimento alimento) {
        if (alimento.getUsuarioBloqueoCocina() == null) {
            return null;
        }
        String nombre = (alimento.getUsuarioBloqueoCocina().getNombre() + " "
                + alimento.getUsuarioBloqueoCocina().getApellido()).trim();
        return nombre.isBlank() ? alimento.getUsuarioBloqueoCocina().getCorreo() : nombre;
    }
}
