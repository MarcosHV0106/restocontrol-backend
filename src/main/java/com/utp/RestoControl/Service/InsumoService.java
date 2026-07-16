
package com.utp.RestoControl.Service;

import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Repository.InsumoRepository;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsumoService {
    
    private final InsumoRepository repository;
    
    @Transactional(readOnly = true)
    public List<Insumo> listar(){
        return repository.findAll();
    }
    @Transactional
    public Insumo guardar(Insumo insumo){
        validar(insumo);
        if (insumo.getCostoUnitario() == null) {
            insumo.setCostoUnitario(BigDecimal.ZERO);
        }
        return repository.save(insumo);
    }

    @Transactional(readOnly = true)
    public Insumo buscarPorId(Integer id) {
        return repository.findByIdInsumo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));
    }

    @Transactional
    public Insumo actualizar(Integer id, Insumo request) {
        validar(request);
        Insumo insumo = buscarPorId(id);
        insumo.setNombreInsumo(request.getNombreInsumo().trim());
        insumo.setUnidadMedida(request.getUnidadMedida().trim());
        insumo.setStockActual(request.getStockActual());
        insumo.setStockMinimo(request.getStockMinimo());
        insumo.setCostoUnitario(request.getCostoUnitario() == null
                ? BigDecimal.ZERO
                : request.getCostoUnitario());
        return repository.save(insumo);
    }

    private void validar(Insumo insumo) {
        Preconditions.checkArgument(insumo != null, "El insumo es obligatorio.");
        Preconditions.checkArgument(insumo.getNombreInsumo() != null
                && !insumo.getNombreInsumo().trim().isEmpty(), "El nombre del insumo es obligatorio.");
        Preconditions.checkArgument(insumo.getUnidadMedida() != null
                && !insumo.getUnidadMedida().trim().isEmpty(), "La unidad de medida es obligatoria.");
        Preconditions.checkArgument(insumo.getCostoUnitario() == null
                || insumo.getCostoUnitario().compareTo(BigDecimal.ZERO) >= 0,
                "El costo unitario no puede ser negativo.");
    }
}
