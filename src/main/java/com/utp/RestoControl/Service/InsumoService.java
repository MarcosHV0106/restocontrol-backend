
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Repository.InsumoRepository;
import java.util.List;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsumoService {

    private final InsumoRepository repository;

    public List<Insumo> listar() {
        return repository.findByEliminadoFalse();
    }

    public Insumo guardar(Insumo insumo) {
        insumo.setStockActual(BigDecimal.ZERO);
        insumo.setFechaVencimiento(null);
        if (insumo.getEliminado() == null) {
            insumo.setEliminado(false);
        }

        return repository.save(insumo);
    }

    public Insumo buscarPorId(Integer id) {
        return repository
                .findByIdInsumoAndEliminadoFalse(id)
                .orElse(null);
    }

    public Insumo actualizar(Integer id, Insumo insumo) {

        Insumo existente = repository
                .findByIdInsumoAndEliminadoFalse(id)
                .orElse(null);

        if (existente == null) {
            return null;
        }

        existente.setNombreInsumo(insumo.getNombreInsumo());
        existente.setDescripcion(insumo.getDescripcion());
        existente.setUnidadMedida(insumo.getUnidadMedida());
        existente.setStockMinimo(insumo.getStockMinimo());
        existente.setCostoUnitario(insumo.getCostoUnitario());

        return repository.save(existente);
    }

    public void eliminar(Integer id) {

        Insumo insumo = repository
                .findByIdInsumoAndEliminadoFalse(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));

        insumo.setEliminado(true);

        repository.save(insumo);

    }
}
