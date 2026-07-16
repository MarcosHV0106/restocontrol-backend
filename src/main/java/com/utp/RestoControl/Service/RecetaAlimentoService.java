package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.utp.RestoControl.Dto.RecetaAlimentoRequest;
import com.utp.RestoControl.Entity.Alimento;
import com.utp.RestoControl.Entity.Insumo;
import com.utp.RestoControl.Entity.RecetaAlimento;
import com.utp.RestoControl.Repository.RecetaAlimentoRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecetaAlimentoService {

    private final RecetaAlimentoRepository repository;
    private final AlimentoService alimentoService;
    private final InsumoService insumoService;

    @Transactional(readOnly = true)
    public List<RecetaAlimento> listar(Integer idAlimento) {
        alimentoService.buscarPorId(idAlimento);
        return repository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(idAlimento);
    }

    @Transactional
    public List<RecetaAlimento> reemplazar(Integer idAlimento, List<RecetaAlimentoRequest> requests) {
        Preconditions.checkArgument(requests != null, "La receta es obligatoria.");
        Alimento alimento = alimentoService.buscarPorId(idAlimento);
        Set<Integer> idsInsumo = new HashSet<>();

        List<RecetaAlimento> receta = requests.stream().map(request -> {
            Preconditions.checkArgument(request != null && request.getIdInsumo() != null,
                    "Cada ingrediente debe indicar un insumo.");
            Preconditions.checkArgument(request.getCantidad() != null
                            && request.getCantidad().compareTo(BigDecimal.ZERO) > 0,
                    "La cantidad de cada ingrediente debe ser mayor a cero.");
            Preconditions.checkArgument(idsInsumo.add(request.getIdInsumo()),
                    "No se puede repetir un insumo en la receta.");

            Insumo insumo = insumoService.buscarPorId(request.getIdInsumo());
            return RecetaAlimento.builder()
                    .alimento(alimento)
                    .insumo(insumo)
                    .cantidad(request.getCantidad())
                    .build();
        }).toList();

        repository.deleteByAlimento_IdAlimento(idAlimento);
        repository.flush();
        repository.saveAll(receta);
        return repository.findByAlimento_IdAlimentoOrderByInsumo_NombreInsumo(idAlimento);
    }
}
