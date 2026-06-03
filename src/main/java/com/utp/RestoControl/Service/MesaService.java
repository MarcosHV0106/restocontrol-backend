
package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Entity.Mesa;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.MesaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MesaService {
    
    private final MesaRepository repository;
    
    @Transactional(readOnly = true)
    public List<Mesa> listar(){
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public Mesa buscarPorId(Integer id) {
        return repository.findByIdMesaAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada."));
    }

    @Transactional
    public Mesa guardar(Mesa mesa){
        validarMesa(mesa, null);
        mesa.setIdMesa(null);
        mesa.setEliminado(false);

        return repository.save(mesa);
    }

    @Transactional
    public Mesa actualizar(Integer id, Mesa datos) {
        Mesa mesa = buscarPorId(id);
        validarMesa(datos, id);

        mesa.setNumeroMesa(datos.getNumeroMesa());
        mesa.setCapacidad(datos.getCapacidad());
        mesa.setEstadoMesa(datos.getEstadoMesa().trim());

        return repository.save(mesa);
    }

    @Transactional
    public void eliminar(Integer id) {
        Mesa mesa = buscarPorId(id);
        mesa.setEliminado(true);
        repository.save(mesa);
    }

    private void validarMesa(Mesa mesa, Integer idActual) {
        Preconditions.checkArgument(mesa != null, "La mesa es obligatoria.");
        Preconditions.checkArgument(mesa.getNumeroMesa() != null && mesa.getNumeroMesa() > 0, "El numero de mesa debe ser mayor a cero.");
        Preconditions.checkArgument(mesa.getCapacidad() != null && mesa.getCapacidad() > 0, "La capacidad debe ser mayor a cero.");

        String estado = mesa.getEstadoMesa() == null ? null : mesa.getEstadoMesa().trim();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(estado), "El estado de la mesa es obligatorio.");

        boolean numeroDuplicado = idActual == null
                ? repository.existsByNumeroMesaAndEliminadoFalse(mesa.getNumeroMesa())
                : repository.existsByNumeroMesaAndIdMesaNotAndEliminadoFalse(mesa.getNumeroMesa(), idActual);

        Preconditions.checkArgument(!numeroDuplicado, "Ya existe una mesa activa con ese numero.");
        mesa.setEstadoMesa(estado);
    }
}
