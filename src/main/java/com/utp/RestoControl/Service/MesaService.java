package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.MesaRequest;
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
    public List<Mesa> listar() {
        return repository.findByEliminadoFalse();
    }

    @Transactional(readOnly = true)
    public Integer contarMesasLibres() {
        return repository.countByEstadoMesaAndEliminadoFalse("libre");
    }

    @Transactional(readOnly = true)
    public Integer contarMesasOcupadas() {
        return repository.countByEstadoMesaAndEliminadoFalse("ocupada");
    }

    @Transactional(readOnly = true)
    public Integer contarMesasReservadas() {
        return repository.countByEstadoMesaAndEliminadoFalse("reservada");
    }

    @Transactional(readOnly = true)
    public Integer contarMesasPorCobrar() {
        return repository.countByEstadoMesaAndEliminadoFalse("cobrar");
    }

    @Transactional(readOnly = true)
    public Mesa buscarPorId(Integer id) {
        return repository.findByIdMesaAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada."));
    }

    @Transactional
    public Mesa guardar(MesaRequest request) {

        validarMesa(request, null);

        Mesa mesa = new Mesa();

        mesa.setNumeroMesa(
                request.getNumeroMesa()
        );

        mesa.setCapacidad(
                request.getCapacidad()
        );

        mesa.setPiso(
                request.getPiso()
        );

        mesa.setEstadoMesa("libre");

        mesa.setEliminado(false);

        return repository.save(mesa);

    }

    @Transactional
    public Mesa actualizar(
            Integer id,
            MesaRequest request) {

        Mesa mesa = buscarPorId(id);

        validarMesa(request, id);

        mesa.setNumeroMesa(
                request.getNumeroMesa()
        );

        mesa.setCapacidad(
                request.getCapacidad()
        );

        mesa.setPiso(
                request.getPiso()
        );

        return repository.save(mesa);

    }

    @Transactional
    public void eliminar(Integer id) {
        Mesa mesa = buscarPorId(id);
        mesa.setEliminado(true);
        repository.save(mesa);
    }

    private void validarMesa(
            MesaRequest mesa,
            Integer idActual) {

        Preconditions.checkArgument(
                mesa != null,
                "La mesa es obligatoria."
        );

        Preconditions.checkArgument(
                mesa.getNumeroMesa() != null
                && mesa.getNumeroMesa() > 0,
                "El numero de mesa debe ser mayor a cero."
        );

        Preconditions.checkArgument(
                mesa.getCapacidad() != null
                && mesa.getCapacidad() > 0,
                "La capacidad debe ser mayor a cero."
        );

        Preconditions.checkArgument(
                mesa.getPiso() != null
                && mesa.getPiso() > 0,
                "El piso debe ser mayor a cero."
        );

        boolean numeroDuplicado
                = idActual == null
                        ? repository.existsByNumeroMesaAndEliminadoFalse(
                                mesa.getNumeroMesa())
                        : repository.existsByNumeroMesaAndIdMesaNotAndEliminadoFalse(
                                mesa.getNumeroMesa(),
                                idActual);

        Preconditions.checkArgument(
                !numeroDuplicado,
                "Ya existe una mesa activa con ese numero."
        );

    }
}
