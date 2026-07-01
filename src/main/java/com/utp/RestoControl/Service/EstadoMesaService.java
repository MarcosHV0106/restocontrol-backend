
package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.EstadoMesaRequest;
import com.utp.RestoControl.Entity.EstadoMesa;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.EstadoMesaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstadoMesaService {

    private final EstadoMesaRepository repository;

    @Transactional(readOnly = true)
    public List<EstadoMesa> listar() {

        return repository.findByEliminadoFalse();

    }

    @Transactional(readOnly = true)
    public EstadoMesa buscarPorId(Integer id) {

        return repository
                .findByIdEstadoMesaAndEliminadoFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estado de mesa no encontrado."
                        )
                );

    }

    @Transactional
    public EstadoMesa guardar(
            EstadoMesaRequest request) {

        validar(request, null);

        EstadoMesa estado = new EstadoMesa();

        estado.setDescripcion(
                request.getDescripcion().trim()
        );

        estado.setEliminado(false);

        return repository.save(estado);

    }

    @Transactional
    public EstadoMesa actualizar(
            Integer id,
            EstadoMesaRequest request) {

        EstadoMesa estado = buscarPorId(id);

        validar(request, id);

        estado.setDescripcion(
                request.getDescripcion().trim()
        );

        return repository.save(estado);

    }

    @Transactional
    public void eliminar(Integer id) {

        EstadoMesa estado = buscarPorId(id);

        estado.setEliminado(true);

        repository.save(estado);

    }

    private void validar(
            EstadoMesaRequest request,
            Integer idActual) {

        Preconditions.checkArgument(
                request != null,
                "El estado es obligatorio."
        );

        String descripcion =
                request.getDescripcion()== null
                ? null
                : request.getDescripcion().trim();

        Preconditions.checkArgument(
                !Strings.isNullOrEmpty(descripcion),
                "El nombre del estado es obligatorio."
        );

        boolean duplicado =
                idActual == null
                ? repository.existsByDescripcionAndEliminadoFalse(descripcion)
                : repository.existsByDescripcionAndIdEstadoMesaNotAndEliminadoFalse(
                        descripcion,
                        idActual
                );

        Preconditions.checkArgument(
                !duplicado,
                "Ya existe un estado con ese nombre."
        );

    }

}