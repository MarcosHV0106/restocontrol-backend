
package com.utp.RestoControl.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.utp.RestoControl.Dto.EstadoPedidoRequest;
import com.utp.RestoControl.Entity.EstadoPedido;
import com.utp.RestoControl.Exception.ResourceNotFoundException;
import com.utp.RestoControl.Repository.EstadoPedidoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EstadoPedidoService {

    private final EstadoPedidoRepository repository;

    @Transactional(readOnly = true)
    public List<EstadoPedido> listar() {

        return repository.findByEliminadoFalse();

    }

    @Transactional(readOnly = true)
    public EstadoPedido buscarPorId(Integer id) {

        return repository
                .findByIdEstadoPedidoAndEliminadoFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Estado de pedido no encontrado."
                        )
                );

    }

    @Transactional
    public EstadoPedido guardar(
            EstadoPedidoRequest request) {

        validar(request, null);

        EstadoPedido estado = new EstadoPedido();

        estado.setNombreEstado(
                request.getNombreEstado().trim()
        );

        estado.setEliminado(false);

        return repository.save(estado);

    }

    @Transactional
    public EstadoPedido actualizar(
            Integer id,
            EstadoPedidoRequest request) {

        EstadoPedido estado = buscarPorId(id);

        validar(request, id);

        estado.setNombreEstado(
                request.getNombreEstado().trim()
        );

        return repository.save(estado);

    }

    @Transactional
    public void eliminar(Integer id) {

        EstadoPedido estado = buscarPorId(id);

        estado.setEliminado(true);

        repository.save(estado);

    }

    private void validar(
            EstadoPedidoRequest request,
            Integer idActual) {

        Preconditions.checkArgument(
                request != null,
                "El estado es obligatorio."
        );

        String nombre =
                request.getNombreEstado() == null
                ? null
                : request.getNombreEstado().trim();

        Preconditions.checkArgument(
                !Strings.isNullOrEmpty(nombre),
                "El nombre del estado es obligatorio."
        );

        boolean duplicado =
                idActual == null
                ? repository.existsByNombreEstadoAndEliminadoFalse(nombre)
                : repository.existsByNombreEstadoAndIdEstadoPedidoNotAndEliminadoFalse(
                        nombre,
                        idActual
                );

        Preconditions.checkArgument(
                !duplicado,
                "Ya existe un estado con ese nombre."
        );

    }

}